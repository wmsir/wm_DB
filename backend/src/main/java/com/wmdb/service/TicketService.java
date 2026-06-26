package com.wmdb.service;

import com.wmdb.engine.DbEnginePlugin;
import com.wmdb.model.DbInstance;
import com.wmdb.model.SqlTicket;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.DbInstanceMapper;
import com.wmdb.mapper.SqlTicketDetailMapper;
import com.wmdb.mapper.SqlTicketMapper;
import com.wmdb.model.SqlTicketDetail;
import com.wmdb.engine.MysqlEngineImpl;
import com.wmdb.engine.DmEngineImpl;
import com.wmdb.engine.OracleEngineImpl;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
 * @author wm
 * @date 2023-10-25
 */
@Service
public class TicketService {

    private final StorageService storageService;
    private final MysqlEngineImpl mysqlEnginePlugin;
    private final DmEngineImpl dmEnginePlugin;
    private final OracleEngineImpl oracleEnginePlugin;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    private final SqlTicketMapper sqlTicketMapper;
    private final SqlTicketDetailMapper sqlTicketDetailMapper;
    private final DbInstanceMapper dbInstanceMapper;
    private final AsyncTicketExecutor asyncTicketExecutor;
    private final SqlLintService sqlLintService;
    private final NotificationService notificationService;

    /**
     * 构造函数注入依赖
     *
     * @param storageService 文件存储服务
     * @param mysqlEnginePlugin MySQL AST 引擎插件
     * @param dmEnginePlugin 达梦 AST 引擎插件
     * @param oracleEnginePlugin Oracle AST 引擎插件
     * @param runtimeService Flowable 运行时服务
     * @param taskService Flowable 任务服务
     * @param sqlTicketMapper 工单 Mapper
     * @param sqlTicketDetailMapper 详情 Mapper
     * @param dbInstanceMapper 实例 Mapper
     * @param asyncTicketExecutor 异步执行器
     */
    public TicketService(StorageService storageService, MysqlEngineImpl mysqlEnginePlugin,
                         DmEngineImpl dmEnginePlugin, OracleEngineImpl oracleEnginePlugin,
                         RuntimeService runtimeService, TaskService taskService,
                         SqlTicketMapper sqlTicketMapper, SqlTicketDetailMapper sqlTicketDetailMapper,
                         DbInstanceMapper dbInstanceMapper, AsyncTicketExecutor asyncTicketExecutor,
                         SqlLintService sqlLintService, NotificationService notificationService) {
        this.storageService = storageService;
        this.mysqlEnginePlugin = mysqlEnginePlugin;
        this.dmEnginePlugin = dmEnginePlugin;
        this.oracleEnginePlugin = oracleEnginePlugin;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.sqlTicketMapper = sqlTicketMapper;
        this.sqlTicketDetailMapper = sqlTicketDetailMapper;
        this.dbInstanceMapper = dbInstanceMapper;
        this.asyncTicketExecutor = asyncTicketExecutor;
        this.sqlLintService = sqlLintService;
        this.notificationService = notificationService;
    }

    /**
     * 根据数据库类型动态选择校验插件
     */
    private DbEnginePlugin getEnginePlugin(String dbType) {
        if ("dameng".equalsIgnoreCase(dbType)) {
            return dmEnginePlugin;
        } else if ("oracle".equalsIgnoreCase(dbType)) {
            return oracleEnginePlugin;
        }
        return mysqlEnginePlugin; // fallback
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

        // Fetch db instance to determine plugin
        DbInstance instance = dbInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new RuntimeException("Target DB instance not found");
        }

        // 2. Pre-Check AST using selected engine plugin
        // Important: use astCheckText which has no appended strings to prevent Druid ParserException
        DbEnginePlugin enginePlugin = getEnginePlugin(instance.getDbType());
        enginePlugin.preCheck(storageResult.getAstCheckText());

        // 2.5 智能化 SQL 审核：执行 EXPLAIN
        sqlLintService.explainCheck(instance, storageResult.getAstCheckText());

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

        // 3.5 Calculate Impact Estimate
        // In a real scenario, this involves analyzing the AST for table stats.
        // Mocking impact estimation for architecture demonstration based on file size
        int estimatedRows = file.getSize() > 1024 * 50 ? 50000 : 50;
        detail.setAffectRowsEstimate(estimatedRows);

        // Save to DB
        sqlTicketMapper.insert(ticket);
        sqlTicketDetailMapper.insert(detail);

        // 4. Start Flowable Process
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", idCard);
        variables.put("ticketId", ticketId);
        variables.put("affectRowsEstimate", estimatedRows);
        // Important: Never pass big SQL or DB password into flow variables

        // Start the actual flowable process. This requires 'sqlApprovalProcess' to be deployed.
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("sqlApprovalProcess", ticket.getBusinessKey(), variables);
        ticket.setFlowInstanceId(processInstance.getId());
        sqlTicketMapper.updateById(ticket);

        // 发送通知
        notificationService.sendTicketNotification(ticket, "SUBMITTED");

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

            // 发送通知
            notificationService.sendTicketNotification(ticket, "APPROVED");

            // 维护窗口判断
            if (ticket.getExecutionWindow() != null && !ticket.getExecutionWindow().isEmpty()) {
                // 如果有维护窗口，暂时先不执行，由定时任务来拉起。模拟状态。
                System.out.println("Ticket " + ticketId + " queued for maintenance window: " + ticket.getExecutionWindow());
            } else {
                // 委托给专门的异步组件执行，避免阻塞回调线程
                asyncTicketExecutor.executeTicket(ticketId);
            }
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
