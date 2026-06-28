package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.model.SqlTicket;
import com.wmdb.service.TicketService;
import com.wmdb.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;
import com.wmdb.model.SqlTicketDetail;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工单控制器
 * <p>
 * 提供工单提交、详情获取、附件下载等前台 BFF 接口。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ticket")
public class TicketController {

    private final TicketService ticketService;

    // Auto-inject MinioClient configured by Spring or manually instance it
    // using properties for presigned URLs.
    @Value("${wmdb.minio.endpoint}")
    private String endpoint;

    @Value("${wmdb.minio.access-key}")
    private String accessKey;

    @Value("${wmdb.minio.secret-key}")
    private String secretKey;

    @Value("${wmdb.minio.bucket}")
    private String bucketName;

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
     * @throws Exception 上传异常或 AST 解析异常
     */
    @PostMapping("/submit")
    public Result<SqlTicket> submitTicket(@RequestParam("instanceId") Long instanceId,
                                          @RequestParam("file") MultipartFile file) throws Exception {
        // Get ID Card from security context (populated by JwtAuthenticationFilter)
        String idCard = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SqlTicket ticket = ticketService.submitTicket(idCard, instanceId, file);
        return Result.success(ticket);
    }

    /**
     * 获取用户工单列表
     *
     * @return 包含工单列表的响应实体
     */
    @GetMapping("/list")
    public Result<List<SqlTicket>> listTickets() {
        String currentIdCard = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(ticketService.listUserTickets(currentIdCard));
    }

    /**
     * 获取工单详情
     *
     * @param id 工单主键 ID
     * @return 包含工单及明细数据的响应实体
     */
    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> getTicketDetail(@PathVariable("id") Long id) {
        String currentIdCard = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> detail = ticketService.getTicketDetail(id, currentIdCard);
        if (detail == null || detail.get("ticket") == null) {
            throw new BusinessException("A0403", "拒绝访问或工单不存在");
        }
        return Result.success(detail);
    }

    /**
     * 获取附件下载预签名链接（防盗链机制，自带越权防护）
     *
     * @param id 工单主键 ID
     * @return 临时防盗链 URL
     * @throws Exception MinIO 异常
     */
    @GetMapping("/{id}/download-url")
    public Result<Map<String, String>> getDownloadUrl(@PathVariable("id") Long id) throws Exception {
        String currentIdCard = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> detailMap = ticketService.getTicketDetail(id, currentIdCard);
        if (detailMap == null || detailMap.get("detail") == null) {
            throw new BusinessException("A0403", "拒绝访问或工单不存在");
        }

        SqlTicketDetail detail = (SqlTicketDetail) detailMap.get("detail");
        String objectKey = detail.getAttachmentOssKey();
        if (objectKey == null) {
            throw new BusinessException("A0404", "此工单没有附件");
        }

        MinioClient minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();

        String url = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(objectKey)
                .expiry(5, TimeUnit.MINUTES)
                .build());

        return Result.success(Map.of("url", url));
    }
}
