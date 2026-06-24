package com.wmdb.service;

import com.wmdb.engine.DbEnginePlugin;
import com.wmdb.model.DbInstance;
import com.wmdb.model.SqlTicket;
import com.wmdb.model.SqlTicketDetail;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketService {

    private final StorageService storageService;
    private final DbEnginePlugin mysqlEnginePlugin;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    // Ideally, injected via mapper, but mocking for now
    private final Map<Long, SqlTicket> ticketDb = new HashMap<>();
    private final Map<Long, SqlTicketDetail> ticketDetailDb = new HashMap<>();
    private final Map<Long, DbInstance> instanceDb = new HashMap<>();

    public TicketService(StorageService storageService, DbEnginePlugin mysqlEnginePlugin,
                         RuntimeService runtimeService, TaskService taskService) {
        this.storageService = storageService;
        this.mysqlEnginePlugin = mysqlEnginePlugin;
        this.runtimeService = runtimeService;
        this.taskService = taskService;

        // Mock DB instance
        DbInstance mockInstance = new DbInstance();
        mockInstance.setId(1L);
        mockInstance.setName("Mock Prod MySQL");
        mockInstance.setDbType("mysql");
        mockInstance.setJdbcUrl("jdbc:mysql://localhost:3306/mockdb");
        mockInstance.setUsername("root");
        mockInstance.setPasswordCipher("mockPassword"); // In reality, AES encrypted
        instanceDb.put(1L, mockInstance);
    }

    public SqlTicket submitTicket(String idCard, Long instanceId, MultipartFile file) throws Exception {
        // 1. Process File
        StorageService.StorageResult storageResult = storageService.processSqlFile(file);

        // 2. Pre-Check AST
        mysqlEnginePlugin.preCheck(storageResult.getSqlText());

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

        // Save (mock)
        ticketDb.put(ticketId, ticket);
        ticketDetailDb.put(ticketId, detail);

        // 4. Start Flowable Process
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant", idCard);
        variables.put("ticketId", ticketId);
        // Important: Never pass big SQL or DB password into flow variables

        // Assuming we have a deployed process named 'sqlApprovalProcess'
        // Since we are not actually deploying it in this skeleton, we comment it out
        // ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("sqlApprovalProcess", ticket.getBusinessKey(), variables);
        // ticket.setFlowInstanceId(processInstance.getId());
        ticket.setFlowInstanceId("mock-flow-instance-id");

        return ticket;
    }

    public void approveTicket(Long ticketId) {
        SqlTicket ticket = ticketDb.get(ticketId);
        if (ticket != null && "AUDITING".equals(ticket.getStatus())) {
            ticket.setStatus("APPROVED");
            executeTicket(ticketId);
        }
    }

    private void executeTicket(Long ticketId) {
        SqlTicket ticket = ticketDb.get(ticketId);
        SqlTicketDetail detail = ticketDetailDb.get(ticketId);
        DbInstance instance = instanceDb.get(ticket.getInstanceId());

        try {
            // Memory decrypt password (mocked here)
            String pwd = instance.getPasswordCipher();

            // JDBC Execution with fetchSize
            try (Connection conn = DriverManager.getConnection(instance.getJdbcUrl(), instance.getUsername(), pwd);
                 Statement stmt = conn.createStatement()) {

                // Protection mechanism for large execution sets
                stmt.setFetchSize(1000);

                // If it's a small script, execute directly
                // If it's large (attachmentOssKey != null), we should stream it from MinIO.
                // In this mock, we just execute the text.
                // stmt.execute(detail.getSqlText());

                ticket.setStatus("EXECUTED");
            } catch (Exception e) {
                System.out.println("Execution failed: " + e.getMessage());
                ticket.setStatus("FAILED");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ticket.setStatus("FAILED");
        }
    }

    public Map<String, Object> getTicketDetail(Long ticketId) {
        Map<String, Object> result = new HashMap<>();
        result.put("ticket", ticketDb.get(ticketId));
        result.put("detail", ticketDetailDb.get(ticketId));
        return result;
    }
}
