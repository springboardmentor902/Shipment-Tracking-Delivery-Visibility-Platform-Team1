package com.shiptrack.shiptrack_pro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateResponse {
    private Long routeId;
    private Long shipmentId;
    private String trackingNumber;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime recordedAt;
}
