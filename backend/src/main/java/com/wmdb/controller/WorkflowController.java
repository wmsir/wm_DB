package com.wmdb.controller;

import com.wmdb.service.TicketService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    private final TicketService ticketService;

    public WorkflowController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // This endpoint should be secured via HMAC or whitelist in production since it's called by OA
    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody WorkflowCallbackRequest request) {
        try {
            if ("APPROVED".equals(request.getStatus())) {
                ticketService.approveTicket(request.getTicketId());
            } else {
                // handle rejection
            }
            return ResponseEntity.ok("Callback processed");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing callback: " + e.getMessage());
        }
    }

    @Data
    public static class WorkflowCallbackRequest {
        private Long ticketId;
        private String status; // APPROVED, REJECTED
        private String comment;
    }
}
