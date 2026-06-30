package com.wmdb.service;

import com.wmdb.model.SqlTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息通知服务
 *
 * @author wm
 */
@Slf4j
@Service
public class NotificationService {

    public void sendTicketNotification(SqlTicket ticket, String status) {
        log.info("Ticket {} status changed to: {}", ticket.getId(), status);
        // 模拟发送 IM、邮件等通知
    }
}
