package com.wmdb.service;

import com.wmdb.model.SqlTicket;
import org.springframework.stereotype.Service;

/**
 * IM 通知聚合服务
 * <p>
 * 适配企业微信、钉钉等企业级 IM 平台，推送卡片消息用于一键审批或状态流转通知。
 * </p>
 *
 * @author wm
 */
@Service
public class NotificationService {

    /**
     * 发送工单流转通知
     *
     * @param ticket 工单实体
     * @param eventType 事件类型（如：SUBMITTED, APPROVED, EXECUTED, FAILED）
     */
    public void sendTicketNotification(SqlTicket ticket, String eventType) {
        // 在商业版中，此处会构造特定的 JSON Body 并发送 HTTP POST 到 IM Webhook
        String message = String.format("【wmDB 工单流转通知】\n工单号：%s\n当前状态：%s\n事件：%s",
            ticket.getBusinessKey(), ticket.getStatus(), eventType);

        // 模拟发送
        System.out.println("---- IM Notification Sent ----");
        System.out.println(message);
        System.out.println("------------------------------");
    }
}
