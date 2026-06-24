package com.wmdb.controller;

import com.wmdb.model.SqlTicket;
import com.wmdb.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 工单控制器
 * <p>
 * 提供工单提交、详情获取、附件下载等前台 BFF 接口。
 * </p>
 *
 * @author Jules
 * @date 2023-10-25
 */
@RestController
@RequestMapping("/api/v1/ticket")
public class TicketController {

    private final TicketService ticketService;

    /**
     * 构造函数注入 TicketService
     *
     * @param ticketService 工单服务
     */
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 提交 SQL 审核工单
     *
     * @param instanceId 目标实例 ID
     * @param file 附件文件（长短文分流）
     * @return 响应包含工单基础信息
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitTicket(@RequestParam("instanceId") Long instanceId,
                                          @RequestParam("file") MultipartFile file) {
        try {
            // Get ID Card from security context (populated by JwtAuthenticationFilter)
            String idCard = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            SqlTicket ticket = ticketService.submitTicket(idCard, instanceId, file);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Submission failed: " + e.getMessage());
        }
    }

    /**
     * 获取工单详情
     *
     * @param id 工单主键 ID
     * @return 包含工单及明细数据的响应实体
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getTicketDetail(@PathVariable("id") Long id) {
        try {
            Map<String, Object> detail = ticketService.getTicketDetail(id);
            if (detail.get("ticket") == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error retrieving details: " + e.getMessage());
        }
    }

    /**
     * 获取附件下载预签名链接（防盗链机制）
     *
     * @param id 工单主键 ID
     * @return 临时防盗链 URL
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<?> getDownloadUrl(@PathVariable("id") Long id) {
        // Pre-signed URL logic should be here. Mocking for now.
        // String url = minioClient.getPresignedObjectUrl(...);
        String mockUrl = "http://localhost:9000/wmdb-attachments/mock-file.sql?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";
        return ResponseEntity.ok(Map.of("url", mockUrl));
    }
}
