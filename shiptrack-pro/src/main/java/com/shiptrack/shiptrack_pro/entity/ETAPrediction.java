package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "eta_predictions",
        indexes = @Index(name = "idx_eta_predictions_shipment", columnList = "shipment_id", unique = true),
        check = @CheckConstraint(
                name = "chk_eta_prediction_scores",
                constraint = "delay_risk_score >= 0 and delay_risk_score <= 10 "
                        + "and confidence_score >= 0 and confidence_score <= 100"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ETAPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true, updatable = false)
    private Shipment shipment;

    @Column(name = "predicted_delivery_time", nullable = false)
    private LocalDateTime predictedDeliveryTime;

    @Column(name = "delay_risk_score", nullable = false, precision = 3, scale = 1)
    private BigDecimal delayRiskScore;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 1)
    private BigDecimal confidenceScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String factors;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Version
    private Long version;
}
