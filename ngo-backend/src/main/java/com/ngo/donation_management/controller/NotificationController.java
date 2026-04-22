package com.ngo.donation_management.controller;

import com.ngo.donation_management.dto.UserNotificationDTO;
import com.ngo.donation_management.service.EmailService;
import com.ngo.donation_management.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserNotificationService userNotificationService;

    @GetMapping("/me")
    public ResponseEntity<List<UserNotificationDTO>> getMyNotifications() {
        return ResponseEntity.ok(userNotificationService.getCurrentUserNotifications());
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of(
                "count",
                userNotificationService.getCurrentUserUnreadCount()
        ));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<UserNotificationDTO> markAsRead(
            @PathVariable Integer notificationId) {
        return ResponseEntity.ok(userNotificationService.markAsRead(notificationId));
    }

    @PutMapping("/me/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        userNotificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("message", "Notifications marked as read"));
    }

    /**
     * POST /api/notifications/test-email
     * Send a test email (admin use only).
     * Body: { "to": "email@example.com", "subject": "Test", "message": "Hello" }
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestBody Map<String, String> body) {
        String to = body.get("to");
        String subject = body.getOrDefault("subject", "Test Email from NGO System");
        String message = body.getOrDefault("message", "This is a test notification.");
        String html = "<div style='font-family:Arial;padding:20px;'><p>" + message + "</p></div>";
        emailService.sendEmail(to, subject, html);
        return ResponseEntity.ok(Map.of("status", "Email sent to " + to));
    }

}
