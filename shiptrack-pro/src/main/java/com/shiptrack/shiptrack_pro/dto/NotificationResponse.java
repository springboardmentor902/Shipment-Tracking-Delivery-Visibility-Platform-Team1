package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.NotificationStatus;
import com.shiptrack.shiptrack_pro.entity.NotificationType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationResponse {
    Long id;
    Long userId;
    Long shipmentId;
    String trackingNumber;
    NotificationType type;
    String title;
    String message;
    NotificationStatus status;
    LocalDateTime sentAt;
    LocalDateTime readAt;
    LocalDateTime createdAt;
    boolean unread;
}
