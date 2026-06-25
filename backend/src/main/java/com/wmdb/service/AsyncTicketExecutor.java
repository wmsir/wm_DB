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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 异步工单执行器
 * <p>
 * 专门用于在后台线程中异步执行耗时的 SQL 脚本，不阻塞主回调线程。
 * </p>
 *
 * @author Jules
 */
@Service
public class AsyncTicketExecutor {

    private final SqlTicketMapper sqlTicketMapper;
    private final SqlTicketDetailMapper sqlTicketDetailMapper;
    private final DbInstanceMapper dbInstanceMapper;
    private final StorageService storageService;
    private final SqlAuditLogMapper sqlAuditLogMapper;

    public AsyncTicketExecutor(SqlTicketMapper sqlTicketMapper, SqlTicketDetailMapper sqlTicketDetailMapper,
                               DbInstanceMapper dbInstanceMapper, StorageService storageService,
                               SqlAuditLogMapper sqlAuditLogMapper) {
        this.sqlTicketMapper = sqlTicketMapper;
        this.sqlTicketDetailMapper = sqlTicketDetailMapper;
        this.dbInstanceMapper = dbInstanceMapper;
        this.storageService = storageService;
        this.sqlAuditLogMapper = sqlAuditLogMapper;
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
        SqlTicket ticket = sqlTicketMapper.selectById(ticketId);
        SqlTicketDetail detail = sqlTicketDetailMapper.selectOne(new QueryWrapper<SqlTicketDetail>().eq("ticket_id", ticketId));
        DbInstance instance = dbInstanceMapper.selectById(ticket.getInstanceId());

        if (ticket == null || detail == null || instance == null) {
            return;
        }

        try {
            // Memory decrypt password (in reality use AES decrypt logic)
            String pwd = instance.getPasswordCipher();

            try (Connection conn = DriverManager.getConnection(instance.getJdbcUrl(), instance.getUsername(), pwd);
                 Statement stmt = conn.createStatement()) {

                // Protection mechanism for large execution sets
                stmt.setFetchSize(1000);

                if (detail.getAttachmentOssKey() == null) {
                    // Small script, execute directly
                    long start = System.currentTimeMillis();
                    try {
                        stmt.execute(detail.getSqlText());
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
                                    stmt.execute(sqlToExecute);
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
                                stmt.execute(sqlToExecute);
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
            } catch (Exception e) {
                System.err.println("JDBC Execution failed: " + e.getMessage());
                ticket.setStatus("FAILED");
                sqlTicketMapper.updateById(ticket);
            }
        } catch (Exception e) {
            System.err.println("Ticket execution process failed: " + e.getMessage());
            ticket.setStatus("FAILED");
            sqlTicketMapper.updateById(ticket);
        }
    }
}
