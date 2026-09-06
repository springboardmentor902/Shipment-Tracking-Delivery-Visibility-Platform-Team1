package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.entity.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EtaCalculationServiceTest {

    private final EtaCalculationService calculationService = new EtaCalculationService();
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 2, 12, 0);

    @Test
    void severeTrafficAndFailedDeliveryProduceExplainableHighRiskPrediction() {
        Shipment shipment = shipment(ShipmentStatus.FAILED_DELIVERY);
        Route route = route(TrafficCondition.SEVERE);
        TrackingEvent oldEvent = TrackingEvent.builder()
                .eventType(TrackingEventType.STATUS_CHANGED)
                .status(ShipmentStatus.FAILED_DELIVERY)
                .recordedAt(now.minusHours(14))
                .build();

        EtaCalculation result = calculationService.calculate(
                shipment, route, List.of(oldEvent), now);

        assertThat(result.predictedDeliveryTime()).isAfter(now);
        assertThat(result.delayRiskScore()).isBetween(
                new BigDecimal("0.0"), new BigDecimal("10.0"));
        assertThat(result.delayRiskScore()).isGreaterThan(new BigDecimal("6.0"));
        assertThat(result.confidenceScore()).isBetween(
                new BigDecimal("0.0"), new BigDecimal("100.0"));
        assertThat(result.factors()).contains(
                "Traffic severe",
                "failed delivery attempt",
                "tracking event(s)",
                "latest tracking update is old"
        );
    }

    @Test
    void outForDeliveryHasEarlierEtaThanNewlyCreatedShipment() {
        Route route = route(TrafficCondition.LIGHT);
        TrackingEvent freshEvent = TrackingEvent.builder()
                .eventType(TrackingEventType.LOCATION_UPDATED)
                .recordedAt(now.minusMinutes(5))
                .build();

        EtaCalculation created = calculationService.calculate(
                shipment(ShipmentStatus.CREATED), route, List.of(freshEvent), now);
        EtaCalculation outForDelivery = calculationService.calculate(
                shipment(ShipmentStatus.OUT_FOR_DELIVERY), route, List.of(freshEvent), now);

        assertThat(outForDelivery.predictedDeliveryTime())
                .isBefore(created.predictedDeliveryTime());
    }

    private Shipment shipment(ShipmentStatus status) {
        return Shipment.builder()
                .status(status)
                .estimatedDeliveryDate(LocalDate.of(2026, 9, 3))
                .build();
    }

    private Route route(TrafficCondition trafficCondition) {
        return Route.builder()
                .distanceKm(new BigDecimal("300.00"))
                .estimatedTimeMinutes(420L)
                .originLatitude(new BigDecimal("18.5204303"))
                .destinationLatitude(new BigDecimal("12.9715987"))
                .trafficCondition(trafficCondition)
                .build();
    }
}
