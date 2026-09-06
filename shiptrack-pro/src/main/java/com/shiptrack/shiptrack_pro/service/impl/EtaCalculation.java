package com.shiptrack.shiptrack_pro.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record EtaCalculation(
        LocalDateTime predictedDeliveryTime,
        BigDecimal delayRiskScore,
        BigDecimal confidenceScore,
        String factors
) {
}
