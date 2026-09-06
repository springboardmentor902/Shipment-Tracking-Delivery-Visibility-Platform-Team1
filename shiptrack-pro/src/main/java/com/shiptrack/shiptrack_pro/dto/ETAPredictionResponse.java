package com.shiptrack.shiptrack_pro.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETAPredictionResponse {
    private Long id;
    private Long shipmentId;
    private String trackingNumber;
    private LocalDateTime predictedDeliveryTime;
    private BigDecimal delayRiskScore;
    private BigDecimal confidenceScore;
    private String factors;
    private LocalDateTime calculatedAt;
    private boolean atRisk;
}
