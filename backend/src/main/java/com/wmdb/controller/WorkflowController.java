package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.service.TicketService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批流回调控制器
 * <p>
 * 提供给外部 OA 或审批系统的 webhook 回调接口，用于触发工单状态流转。
 * </p>
 *
 * @author Jules
 * @date 2023-10-25
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    private final TicketService ticketService;

    /**
     * 构造函数注入 TicketService
     *
     * @param ticketService 工单服务
     */
    public WorkflowController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 接收外部审批系统的回调
     * <p>
     * 生产环境中该接口必须通过 HMAC 签名或 IP 白名单进行鉴权保护。
     * </p>
     *
     * @param request 回调请求参数体
     * @return 响应结果
     */
    @PostMapping("/callback")
    public Result<String> callback(@RequestBody WorkflowCallbackRequest request) {
        if ("APPROVED".equals(request.getStatus())) {
            ticketService.approveTicket(request.getTicketId());
        } else {
            // handle rejection
        }
        return Result.success("Callback processed");
    }

    /**
     * 工作流回调请求参数实体
     */
    @Data
    public static class WorkflowCallbackRequest {
        /**
         * 工单 ID
         */
        private Long ticketId;

        /**
         * 审批状态（如：APPROVED, REJECTED）
         */
        private String status;

        /**
         * 审批意见
         */
        private String comment;
    }
}
