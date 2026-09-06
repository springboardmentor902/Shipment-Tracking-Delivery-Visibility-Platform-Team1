package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.NotificationRequest;
import com.shiptrack.shiptrack_pro.dto.NotificationResponse;
import com.shiptrack.shiptrack_pro.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getForUser(authentication.getName()));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, authentication.getName()));
    }

    @PostMapping("/api/notification")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(request, authentication.getName()));
    }
}
