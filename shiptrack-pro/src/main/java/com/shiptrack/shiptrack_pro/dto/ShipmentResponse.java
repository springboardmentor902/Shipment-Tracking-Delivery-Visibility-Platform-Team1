package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private Long id;
    private String trackingNumber;
    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private String receiverName;
    private String receiverPhone;
    private String receiverEmail;
    private String receiverAddress;
    private String pickupAddress;
    private String deliveryAddress;
    private ShipmentPriority priority;
    private List<PackageResponse> packages;
    private ShipmentStatus status;
    private String currentLocation;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String cancellationReason;
    private Long createdById;
    private String createdBy;
    private Long assignedOperatorId;
    private String assignedOperator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
