package com.wmdb.service;

import com.wmdb.engine.DbEnginePlugin;
import com.wmdb.model.DbInstance;
import com.wmdb.model.SqlTicket;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.DbInstanceMapper;
import com.wmdb.mapper.SqlTicketDetailMapper;
import com.wmdb.mapper.SqlTicketMapper;
import com.wmdb.model.SqlTicketDetail;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.minio.GetObjectArgs;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 工单管理服务
 * <p>
 * 核心调度中台，负责工单创建、文件处理、AST预检、流转 Flowable 引擎，
 * 以及工单审批通过后的安全下发执行逻辑。
 * </p>
 *
 * @author Jules
 * @date 2023-10-25
 */
@Service
public class TicketService {

    private final StorageService storageService;
    private final DbEnginePlugin mysqlEnginePlugin;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    private final SqlTicketMapper sqlTicketMapper;
    private final SqlTicketDetailMapper sqlTicketDetailMapper;
    private final DbInstanceMapper dbInstanceMapper;

    /**
     * 构造函数注入依赖
     *
     * @param storageService 文件存储服务
     * @param mysqlEnginePlugin MySQL AST 引擎插件
     * @param runtimeService Flowable 运行时服务
     * @param taskService Flowable 任务服务
     * @param sqlTicketMapper 工单 Mapper
     * @param sqlTicketDetailMapper 详情 Mapper
     * @param dbInstanceMapper 实例 Mapper
     */
    public TicketService(StorageService storageService, DbEnginePlugin mysqlEnginePlugin,
                         RuntimeService runtimeService, TaskService taskService,
                         SqlTicketMapper sqlTicketMapper, SqlTicketDetailMapper sqlTicketDetailMapper,
                         DbInstanceMapper dbInstanceMapper) {
        this.storageService = storageService;
        this.mysqlEnginePlugin = mysqlEnginePlugin;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.sqlTicketMapper = sqlTicketMapper;
        this.sqlTicketDetailMapper = sqlTicketDetailMapper;
        this.dbInstanceMapper = dbInstanceMapper;
    }

    /**
     * 提交变更工单
     *
     * @param idCard 申请人身份证号码
     * @param instanceId 目标数据库实例 ID
     * @param file 上传的 SQL 附件
     * @return 创建成功的工单实例
     * @throws Exception 处理异常
     */
    @Transactional(rollbackFor = Exception.class)
    public SqlTicket submitTicket(String idCard, Long instanceId, MultipartFile file) throws Exception {
        // 1. Process File
        StorageService.StorageResult storageResult = storageService.processSqlFile(file);

        // 2. Pre-Check AST
        // Important: use astCheckText which has no appended strings to prevent Druid ParserException
        mysqlEnginePlugin.preCheck(storageResult.getAstCheckText());

        // 3. Create Ticket
        Long ticketId = System.currentTimeMillis();
        SqlTicket ticket = new SqlTicket();
        ticket.setId(ticketId);
        ticket.setApplicantIdCard(idCard);
        ticket.setInstanceId(instanceId);
        ticket.setStatus("AUDITING");
        ticket.setBusinessKey(UUID.randomUUID().toString());

        SqlTicketDetail detail = new SqlTicketDetail();
        detail.setId(System.currentTimeMillis());
        detail.setTicketId(ticketId);
        detail.setSqlText(storageResult.getSqlText());
        detail.setAttachmentOssKey(storageResult.getAttachmentOssKey());

        // Save to DB
        sqlTicketMapper.insert(ticket);
        sqlTicketDetailMapper.insert(detail);

        // 4. Start Flowable Process
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", idCard);
        variables.put("ticketId", ticketId);
        // Important: Never pass big SQL or DB password into flow variables

        // Start the actual flowable process. This requires 'sqlApprovalProcess' to be deployed.
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("sqlApprovalProcess", ticket.getBusinessKey(), variables);
        ticket.setFlowInstanceId(processInstance.getId());
        sqlTicketMapper.updateById(ticket);

        return ticket;
    }

    /**
     * 审批通过工单（由回调触发）
     *
     * @param ticketId 工单 ID
     */
    public void approveTicket(Long ticketId) {
        SqlTicket ticket = sqlTicketMapper.selectById(ticketId);
        if (ticket != null && "AUDITING".equals(ticket.getStatus())) {
            ticket.setStatus("APPROVED");
            sqlTicketMapper.updateById(ticket);
            executeTicket(ticketId);
        }
    }

    /**
     * 内部执行工单逻辑（包含安全防护和流式执行）
     *
     * @param ticketId 工单 ID
     */
    private void executeTicket(Long ticketId) {
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
                    stmt.execute(detail.getSqlText());
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
                                stmt.execute(sqlBuffer.toString());
                                sqlBuffer.setLength(0); // clear buffer
                            }
                        }
                        // Execute any remaining SQL
                        if (sqlBuffer.length() > 0 && !sqlBuffer.toString().trim().isEmpty()) {
                            stmt.execute(sqlBuffer.toString());
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

    /**
     * 获取工单聚合详情 (带权限校验)
     *
     * @param ticketId 工单 ID
     * @param currentIdCard 当前登录用户的身份证号
     * @return 包含主表和明细表的 Map 数据
     */
    public Map<String, Object> getTicketDetail(Long ticketId, String currentIdCard) {
        SqlTicket ticket = sqlTicketMapper.selectById(ticketId);

        // IDOR 防护：检查工单是否存在，并且申请人必须是当前操作用户
        if (ticket == null || !ticket.getApplicantIdCard().equals(currentIdCard)) {
            // 返回 null 交由 Controller 处理为 404 或 403
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ticket", ticket);
        result.put("detail", sqlTicketDetailMapper.selectOne(new QueryWrapper<SqlTicketDetail>().eq("ticket_id", ticketId)));
        return result;
    }
}
