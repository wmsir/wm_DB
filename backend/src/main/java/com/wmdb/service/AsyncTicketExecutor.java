package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.DbInstanceMapper;
import com.wmdb.mapper.SqlAuditLogMapper;
import com.wmdb.mapper.SqlTicketDetailMapper;
import com.wmdb.mapper.SqlTicketMapper;
import com.wmdb.model.DbInstance;
import com.wmdb.model.SqlAuditLog;
import com.wmdb.model.SqlTicket;
import com.wmdb.model.SqlTicketDetail;
import io.minio.GetObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * 异步工单执行器
 * <p>
 * 专门用于在后台线程中异步执行耗时的 SQL 脚本，不阻塞主回调线程。
 * </p>
 *
 * @author wm
 */
@Slf4j
@Service
public class AsyncTicketExecutor {

    private final SqlTicketMapper sqlTicketMapper;
    private final SqlTicketDetailMapper sqlTicketDetailMapper;
    private final DbInstanceMapper dbInstanceMapper;
    private final StorageService storageService;
    private final SqlAuditLogMapper sqlAuditLogMapper;
    private final DataSource dataSource;
    private final DefaultDataSourceCreator dataSourceCreator;
    private final DataMaskingService dataMaskingService;
    private final NotificationService notificationService;
    private final BlockchainService blockchainService;
    private final AiAgentPipelineService aiAgentPipelineService;

    @Value("${wmdb.db.aes-key:1234567890abcdef1234567890abcdef}") // Default hex key for SM4
    private String aesKey;

    public AsyncTicketExecutor(SqlTicketMapper sqlTicketMapper, SqlTicketDetailMapper sqlTicketDetailMapper,
                               DbInstanceMapper dbInstanceMapper, StorageService storageService,
                               SqlAuditLogMapper sqlAuditLogMapper, DataSource dataSource,
                               DefaultDataSourceCreator dataSourceCreator, DataMaskingService dataMaskingService,
                               NotificationService notificationService, BlockchainService blockchainService,
                               AiAgentPipelineService aiAgentPipelineService) {
        this.sqlTicketMapper = sqlTicketMapper;
        this.sqlTicketDetailMapper = sqlTicketDetailMapper;
        this.dbInstanceMapper = dbInstanceMapper;
        this.storageService = storageService;
        this.sqlAuditLogMapper = sqlAuditLogMapper;
        this.dataSource = dataSource;
        this.dataSourceCreator = dataSourceCreator;
        this.dataMaskingService = dataMaskingService;
        this.notificationService = notificationService;
        this.blockchainService = blockchainService;
        this.aiAgentPipelineService = aiAgentPipelineService;
    }

    /**
     * 判断是否为纯 DQL 查询语句
     */
    private boolean isDqlOnly(String sql, String dbTypeStr) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        try {
            com.alibaba.druid.DbType dbType;
            if ("mysql".equalsIgnoreCase(dbTypeStr)) {
                dbType = com.alibaba.druid.DbType.mysql;
            } else if ("dameng".equalsIgnoreCase(dbTypeStr)) {
                dbType = com.alibaba.druid.DbType.dm;
            } else if ("oracle".equalsIgnoreCase(dbTypeStr)) {
                dbType = com.alibaba.druid.DbType.oracle;
            } else {
                dbType = com.alibaba.druid.DbType.mysql;
            }
            java.util.List<com.alibaba.druid.sql.ast.SQLStatement> statements = com.alibaba.druid.sql.SQLUtils.parseStatements(sql, dbType);
            for (com.alibaba.druid.sql.ast.SQLStatement stmt : statements) {
                if (!(stmt instanceof com.alibaba.druid.sql.ast.statement.SQLSelectStatement)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false; // 解析失败则保守认为是写操作
        }
    }

    /**
     * 处理结果集脱敏打印 (简化版，仅提取第一列或展示脱敏逻辑)
     */
    private void processResultSet(Statement stmt) throws Exception {
        try (java.sql.ResultSet rs = stmt.getResultSet()) {
            if (rs != null) {
                java.sql.ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                // 仅扫描前 10 行用作示例
                int count = 0;
                while (rs.next() && count < 10) {
                    StringBuilder rowData = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        String rawValue = rs.getString(i);
                        String maskedValue = dataMaskingService.mask(rawValue);
                        rowData.append(metaData.getColumnName(i)).append(": ").append(maskedValue).append(" | ");
                    }
                    System.out.println("Row " + count + ": " + rowData.toString());
                    count++;
                }
            }
        }
    }

    /**
     * 保存单条 SQL 执行审计日志，并使用 SM3 构建防篡改哈希链
     */
    private void saveAuditLog(Long ticketId, String sql, long costMs, String status, String errorTrace) {
        SqlAuditLog log = new SqlAuditLog();
        log.setTicketId(ticketId);
        log.setExecuteSql(sql);
        log.setCostTimeMs(costMs);
        log.setStatus(status);
        log.setErrorTrace(errorTrace);

        try {
            // Retrieve the last log to form the chain
            SqlAuditLog lastLog = sqlAuditLogMapper.selectOne(new QueryWrapper<SqlAuditLog>().orderByDesc("id").last("LIMIT 1"));
            String prevHash = (lastLog != null && lastLog.getCurrentHash() != null) ? lastLog.getCurrentHash() : "0000000000000000000000000000000000000000000000000000000000000000";
            log.setPreviousHash(prevHash);

            // Compute SM3 hash of (prevHash + ticketId + status + sql)
            String rawStr = prevHash + ticketId + status + sql;
            String currentHash = com.wmdb.security.SmUtils.sm3Hash(rawStr);
            log.setCurrentHash(currentHash);

            sqlAuditLogMapper.insert(log);
        } catch (Exception e) {
            System.err.println("Failed to insert audit log with hash chain: " + e.getMessage());
        }
    }

    /**
     * 内部执行工单逻辑（包含安全防护和流式执行，异步调用）
     *
     * @param ticketId 工单 ID
     */
    @Async
    public void executeTicket(Long ticketId) {
        // Mock ELK/Skywalking Trace ID
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put("traceId", traceId);

        SqlTicket ticket = sqlTicketMapper.selectById(ticketId);
        SqlTicketDetail detail = sqlTicketDetailMapper.selectOne(new QueryWrapper<SqlTicketDetail>().eq("ticket_id", ticketId));
        DbInstance instance = dbInstanceMapper.selectById(ticket.getInstanceId());

        if (ticket == null || detail == null || instance == null) {
            return;
        }

        // 安全修复：仅允许自动化 SQL 类型进入 JDBC 执行阶段
        java.util.List<String> autoExecutableTypes = java.util.Arrays.asList("SQL_AUDIT", "DATA_RECOVERY");
        if (!autoExecutableTypes.contains(ticket.getType())) {
            log.info("工单 {} 为非自动化 SQL 执行类型 ({})，需转为人工处理通道，安全跳过 JDBC 引擎自动执行步骤。", ticketId, ticket.getType());
            ticket.setStatus("MANUAL_PROCESSING"); // 标记为需人工处理
            sqlTicketMapper.updateById(ticket);
            notificationService.sendTicketNotification(ticket, "MANUAL_PROCESSING");
            return;
        }

        // Update status to EXECUTING before starting
        ticket.setStatus("EXECUTING");
        sqlTicketMapper.updateById(ticket);

        long ticketStartTime = System.currentTimeMillis();
        boolean isOverallSuccess = true;
        String executionErrorStr = null;

        try {
            // Memory decrypt password using SM4.
            // In a real scenario, instance.getPasswordCipher() might fail if it's the scaffolding 'mockPassword'.
            // Ensure aesKey is a valid Hex string (e.g. 32 chars for 128-bit key).
            String pwd;
            try {
                if ("mockPassword".equals(instance.getPasswordCipher())) {
                    pwd = "root"; // Fallback for scaffold
                } else {
                    pwd = com.wmdb.security.SmUtils.sm4Decrypt(instance.getPasswordCipher(), aesKey);
                }
            } catch (Exception e) {
                pwd = instance.getPasswordCipher(); // Fallback if not encrypted during early dev
            }

            // Dynamic Datasource Integration
            String dsKey = "ds_" + instance.getId();
            DataSourceProperty dsp = new DataSourceProperty();
            dsp.setPoolName(dsKey);
            dsp.setUrl(instance.getJdbcUrl());
            dsp.setUsername(instance.getUsername());
            dsp.setPassword(pwd);

            // Determine driver dynamically
            if ("mysql".equalsIgnoreCase(instance.getDbType()) || "tidb".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else if ("dameng".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("dm.jdbc.driver.DmDriver");
            } else if ("oracle".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("oracle.jdbc.OracleDriver");
            } else if ("kingbase".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("com.kingbase8.Driver");
            } else if ("oceanbase".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("com.alipay.oceanbase.jdbc.Driver"); // or mysql driver depending on tenant
            } else if ("opengauss".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("org.opengauss.Driver");
            } else {
                dsp.setDriverClassName("com.mysql.cj.jdbc.Driver"); // fallback
            }

            // Using nanoTime for concurrent safety
            dsKey = dsKey + "_" + System.nanoTime();
            dsp.setPoolName(dsKey);

            DynamicRoutingDataSource drds = (DynamicRoutingDataSource) dataSource;
            DataSource newDataSource = dataSourceCreator.createDataSource(dsp);
            drds.addDataSource(dsKey, newDataSource);

            // 智能读写分离逻辑
            boolean isReadOnlyScript = isDqlOnly(detail.getSqlText(), instance.getDbType());
            String roDsKey = "ro_ds_" + instance.getId() + "_" + System.nanoTime();
            if (isReadOnlyScript && instance.getReadOnlyJdbcUrl() != null && !instance.getReadOnlyJdbcUrl().isEmpty()) {
                DataSourceProperty roDsp = new DataSourceProperty();
                roDsp.setPoolName(roDsKey);
                roDsp.setUrl(instance.getReadOnlyJdbcUrl());
                roDsp.setUsername(instance.getUsername());
                roDsp.setPassword(pwd);
                roDsp.setDriverClassName(dsp.getDriverClassName());
                DataSource roDataSource = dataSourceCreator.createDataSource(roDsp);
                drds.addDataSource(roDsKey, roDataSource);
                DynamicDataSourceContextHolder.push(roDsKey);
            } else {
                DynamicDataSourceContextHolder.push(dsKey);
            }

            try (Connection conn = drds.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Protection mechanism for large execution sets
                stmt.setFetchSize(1000);

                if (detail.getAttachmentOssKey() == null) {
                    // Small script, execute directly
                    long start = System.currentTimeMillis();
                    try {
                        boolean isSelect = stmt.execute(detail.getSqlText());
                        if (isSelect) {
                            processResultSet(stmt);
                        } else {
                            // DML Flashback logic hook
                        }

                        blockchainService.preserveEvidence(ticketId, detail.getSqlText(), ticket.getApplicantIdCard());
                        saveAuditLog(ticketId, detail.getSqlText(), System.currentTimeMillis() - start, "SUCCESS", null);
                    } catch (Exception e) {
                        saveAuditLog(ticketId, detail.getSqlText(), System.currentTimeMillis() - start, "FAILED", e.getMessage());
                        executionErrorStr = e.getMessage();
                        isOverallSuccess = false;

                        // 【AI Agent Pipeline - 自动回滚 & 持续学习】
                        aiAgentPipelineService.autoRollback(ticket, detail.getSqlText(), e.getMessage());
                        aiAgentPipelineService.continuousLearning(detail.getSqlText(), e.getMessage());

                        throw e; // rethrow to fail the overall ticket
                    }
                } else {
                    // Large script, stream from MinIO and execute
                    try (InputStream stream = storageService.getMinioClient().getObject(
                            GetObjectArgs.builder()
                                .bucket(storageService.getBucketName())
                                .object(detail.getAttachmentOssKey())
                                .build());
                         BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                        StringBuilder sqlBuffer = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty() || line.trim().startsWith("--")) {
                                continue;
                            }
                            sqlBuffer.append(line).append("\n");
                            // Basic logic: if line ends with semicolon, execute the buffer
                            if (line.trim().endsWith(";")) {
                                String sqlToExecute = sqlBuffer.toString();
                                long start = System.currentTimeMillis();
                                try {
                                    boolean isSelect = stmt.execute(sqlToExecute);
                                    if (isSelect) processResultSet(stmt);

                                    blockchainService.preserveEvidence(ticketId, sqlToExecute, ticket.getApplicantIdCard());
                                    saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "SUCCESS", null);
                                } catch (Exception e) {
                                    saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "FAILED", e.getMessage());
                                    executionErrorStr = e.getMessage();
                                    isOverallSuccess = false;

                                    aiAgentPipelineService.autoRollback(ticket, sqlToExecute, e.getMessage());
                                    aiAgentPipelineService.continuousLearning(sqlToExecute, e.getMessage());
                                    throw e;
                                }
                                sqlBuffer.setLength(0); // clear buffer
                            }
                        }
                        // Execute any remaining SQL
                        if (sqlBuffer.length() > 0 && !sqlBuffer.toString().trim().isEmpty()) {
                            String sqlToExecute = sqlBuffer.toString();
                            long start = System.currentTimeMillis();
                            try {
                                boolean isSelect = stmt.execute(sqlToExecute);
                                if (isSelect) processResultSet(stmt);

                                blockchainService.preserveEvidence(ticketId, sqlToExecute, ticket.getApplicantIdCard());
                                saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "SUCCESS", null);
                            } catch (Exception e) {
                                saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "FAILED", e.getMessage());
                                executionErrorStr = e.getMessage();
                                isOverallSuccess = false;
                                aiAgentPipelineService.autoRollback(ticket, sqlToExecute, e.getMessage());
                                aiAgentPipelineService.continuousLearning(sqlToExecute, e.getMessage());
                                throw e;
                            }
                        }
                    }
                }

                // 执行成功后的逻辑
                ticket.setStatus("EXECUTED");
                sqlTicketMapper.updateById(ticket);
                notificationService.sendTicketNotification(ticket, "EXECUTED");

                // 【AI Agent Pipeline - 自动监控】
                boolean hasAnomaly = aiAgentPipelineService.autoMonitorAfterRelease(ticketId, instance.getId());
                if (hasAnomaly) {
                    log.warn("[AI Agent Pipeline] 发布后发现性能异常告警，正在执行兜底回滚策略...");
                    // 对小工单模拟智能分析全量内容进行回滚
                    aiAgentPipelineService.autoRollback(ticket, detail.getSqlText() != null ? detail.getSqlText() : "", "Post-release performance anomaly detected.");
                }

            } catch (Exception e) {
                log.error("JDBC Execution failed: ", e);
                ticket.setStatus("FAILED");
                sqlTicketMapper.updateById(ticket);
                notificationService.sendTicketNotification(ticket, "FAILED");
            } finally {
                // Clear context and remove dynamic data source
                String currentDsKey = DynamicDataSourceContextHolder.peek();
                DynamicDataSourceContextHolder.poll();
                if (currentDsKey != null && drds.getDataSources().containsKey(currentDsKey)) {
                     drds.removeDataSource(currentDsKey);
                }
                if (drds.getDataSources().containsKey(dsKey)) {
                     drds.removeDataSource(dsKey);
                }
                if (drds.getDataSources().containsKey(roDsKey)) {
                     drds.removeDataSource(roDsKey);
                }
            }
        } catch (Exception e) {
            log.error("Ticket execution process failed: ", e);
            ticket.setStatus("FAILED");
            sqlTicketMapper.updateById(ticket);
            notificationService.sendTicketNotification(ticket, "FAILED");
        } finally {
            long totalTimeMs = System.currentTimeMillis() - ticketStartTime;

            // 【AI Agent Pipeline - 自动生成报告】
            // 无论成功还是失败，均汇总执行链路生成执行评估报告
            aiAgentPipelineService.autoGenerateReport(ticket, isOverallSuccess, totalTimeMs);

            MDC.clear();
        }
    }
}
