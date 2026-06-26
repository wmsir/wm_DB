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

    @Value("${wmdb.db.aes-key}")
    private String aesKey;

    public AsyncTicketExecutor(SqlTicketMapper sqlTicketMapper, SqlTicketDetailMapper sqlTicketDetailMapper,
                               DbInstanceMapper dbInstanceMapper, StorageService storageService,
                               SqlAuditLogMapper sqlAuditLogMapper, DataSource dataSource,
                               DefaultDataSourceCreator dataSourceCreator, DataMaskingService dataMaskingService,
                               NotificationService notificationService) {
        this.sqlTicketMapper = sqlTicketMapper;
        this.sqlTicketDetailMapper = sqlTicketDetailMapper;
        this.dbInstanceMapper = dbInstanceMapper;
        this.storageService = storageService;
        this.sqlAuditLogMapper = sqlAuditLogMapper;
        this.dataSource = dataSource;
        this.dataSourceCreator = dataSourceCreator;
        this.dataMaskingService = dataMaskingService;
        this.notificationService = notificationService;
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
     * 保存单条 SQL 执行审计日志
     */
    private void saveAuditLog(Long ticketId, String sql, long costMs, String status, String errorTrace) {
        SqlAuditLog log = new SqlAuditLog();
        log.setTicketId(ticketId);
        log.setExecuteSql(sql);
        log.setCostTimeMs(costMs);
        log.setStatus(status);
        log.setErrorTrace(errorTrace);
        // Hash chain logic is omitted for the scaffold, setting dummy hash
        log.setCurrentHash("dummy-hash");
        try {
            sqlAuditLogMapper.insert(log);
        } catch (Exception e) {
            System.err.println("Failed to insert audit log: " + e.getMessage());
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

        try {
            // Memory decrypt password using AES
            String pwd;
            try {
                // If it's the mock password "mockPassword" from the skeleton or unable to decrypt, fallback
                if ("mockPassword".equals(instance.getPasswordCipher())) {
                    pwd = "root"; // dummy fallback for demo to prevent driver connection failure if locally tested
                } else {
                    SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                    byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(instance.getPasswordCipher()));
                    pwd = new String(decrypted, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                // Fallback to raw string if AES fails during scaffolding/demo execution
                pwd = instance.getPasswordCipher();
            }

            // Dynamic Datasource Integration
            String dsKey = "ds_" + instance.getId();
            DataSourceProperty dsp = new DataSourceProperty();
            dsp.setPoolName(dsKey);
            dsp.setUrl(instance.getJdbcUrl());
            dsp.setUsername(instance.getUsername());
            dsp.setPassword(pwd);

            // Determine driver dynamically
            if ("mysql".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else if ("dameng".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("dm.jdbc.driver.DmDriver");
            } else if ("oracle".equalsIgnoreCase(instance.getDbType())) {
                dsp.setDriverClassName("oracle.jdbc.OracleDriver");
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
                        saveAuditLog(ticketId, detail.getSqlText(), System.currentTimeMillis() - start, "SUCCESS", null);
                    } catch (Exception e) {
                        saveAuditLog(ticketId, detail.getSqlText(), System.currentTimeMillis() - start, "FAILED", e.getMessage());
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
                                    saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "SUCCESS", null);
                                } catch (Exception e) {
                                    saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "FAILED", e.getMessage());
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
                                saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "SUCCESS", null);
                            } catch (Exception e) {
                                saveAuditLog(ticketId, sqlToExecute, System.currentTimeMillis() - start, "FAILED", e.getMessage());
                                throw e;
                            }
                        }
                    }
                }

                ticket.setStatus("EXECUTED");
                sqlTicketMapper.updateById(ticket);
                notificationService.sendTicketNotification(ticket, "EXECUTED");
            } catch (Exception e) {
                System.err.println("JDBC Execution failed: " + e.getMessage());
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
            System.err.println("Ticket execution process failed: " + e.getMessage());
            ticket.setStatus("FAILED");
            sqlTicketMapper.updateById(ticket);
            notificationService.sendTicketNotification(ticket, "FAILED");
        } finally {
            MDC.clear();
        }
    }
}
