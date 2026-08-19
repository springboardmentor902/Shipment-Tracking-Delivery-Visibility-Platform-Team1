package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private Long id;
    private Long shipmentId;
    private String trackingNumber;
    private String originAddress;
    private String destinationAddress;
    private BigDecimal originLatitude;
    private BigDecimal originLongitude;
    private BigDecimal destinationLatitude;
    private BigDecimal destinationLongitude;
    private BigDecimal distanceKm;
    private Long estimatedTimeMinutes;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private Long createdById;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
