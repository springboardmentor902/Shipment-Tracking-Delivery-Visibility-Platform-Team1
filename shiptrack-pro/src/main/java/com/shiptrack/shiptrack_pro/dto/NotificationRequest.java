package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;
}
