package com.wmdb.controller;

import com.wmdb.model.SqlTicket;
import com.wmdb.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ticket")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

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

    @GetMapping("/{id}/download-url")
    public ResponseEntity<?> getDownloadUrl(@PathVariable("id") Long id) {
        // Pre-signed URL logic should be here. Mocking for now.
        // String url = minioClient.getPresignedObjectUrl(...);
        String mockUrl = "http://localhost:9000/wmdb-attachments/mock-file.sql?X-Amz-Algorithm=AWS4-HMAC-SHA256&...";
        return ResponseEntity.ok(Map.of("url", mockUrl));
    }
}
