package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String packageDescription;
    private BigDecimal weightKg;
    private String dimensions;
    private Integer quantity;
    private BigDecimal declaredValue;
    private Boolean fragile;
    private ShipmentStatus status;
    private String currentLocation;
    private LocalDate estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private String cancellationReason;
    private Long createdById;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
