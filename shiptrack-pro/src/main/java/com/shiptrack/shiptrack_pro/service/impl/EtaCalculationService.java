package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.entity.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EtaCalculationService {

    EtaCalculation calculate(
            Shipment shipment,
            Route route,
            List<TrackingEvent> history,
            LocalDateTime calculatedAt
    ) {
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            LocalDateTime deliveredAt = shipment.getActualDeliveryDate() == null
                    ? calculatedAt : shipment.getActualDeliveryDate();
            return new EtaCalculation(
                    deliveredAt,
                    BigDecimal.ZERO.setScale(1),
                    BigDecimal.valueOf(100).setScale(1),
                    "Shipment is delivered; actual delivery time is used as the final ETA."
            );
        }

        List<String> factors = new ArrayList<>();
        long totalTravelMinutes = resolveTravelMinutes(route, shipment, calculatedAt, factors);
        double progressFactor = progressFactor(shipment.getStatus());
        long remainingTravelMinutes = Math.max(15, Math.round(totalTravelMinutes * progressFactor));

        TrafficCondition traffic = route.getTrafficCondition() == null
                ? TrafficCondition.UNKNOWN : route.getTrafficCondition();
        double trafficMultiplier = trafficMultiplier(traffic);
        remainingTravelMinutes = Math.round(remainingTravelMinutes * trafficMultiplier);
        factors.add("Traffic " + traffic.name().toLowerCase().replace('_', ' ')
                + " applies a " + formatMultiplier(trafficMultiplier) + " travel-time multiplier.");

        long handlingMinutes = handlingMinutes(shipment.getStatus());
        if (handlingMinutes > 0) {
            remainingTravelMinutes += handlingMinutes;
            factors.add(handlingMinutes + " minutes added for the current "
                    + shipment.getStatus().name().toLowerCase().replace('_', ' ') + " stage.");
        }

        TrackingEvent latestEvent = history.isEmpty() ? null : history.get(history.size() - 1);
        long staleHours = latestEvent == null
                ? 24 : Math.max(0, Duration.between(latestEvent.getRecordedAt(), calculatedAt).toHours());
        long staleBuffer = staleHours > 12 ? 90 : staleHours > 6 ? 45 : staleHours > 3 ? 20 : 0;
        remainingTravelMinutes += staleBuffer;
        if (latestEvent == null) {
            factors.add("No tracking history is available yet, so prediction confidence is reduced.");
        } else {
            factors.add(history.size() + " tracking event(s) considered; latest update is "
                    + staleHours + " hour(s) old.");
        }
        if (staleBuffer > 0) {
            factors.add(staleBuffer + " minutes added because the latest tracking update is old.");
        }

        LocalDateTime predictedDeliveryTime = calculatedAt.plusMinutes(remainingTravelMinutes);
        double risk = trafficRisk(traffic) + staleRisk(staleHours, latestEvent == null);
        if (shipment.getStatus() == ShipmentStatus.FAILED_DELIVERY) {
            risk += 3.0;
            factors.add("A failed delivery attempt increases delay risk.");
        }

        LocalDateTime promisedTime = shipment.getEstimatedDeliveryDate().atTime(LocalTime.MAX);
        long lateMinutes = Duration.between(promisedTime, predictedDeliveryTime).toMinutes();
        if (lateMinutes > 0) {
            double deadlineRisk = Math.min(3.5, Math.max(1.0, lateMinutes / 1440.0));
            risk += deadlineRisk;
            factors.add("Predicted arrival is after the promised delivery date.");
        } else {
            factors.add("Predicted arrival remains within the promised delivery date.");
        }

        double confidence = 40;
        if (route.getEstimatedTimeMinutes() != null) confidence += 20;
        else if (route.getDistanceKm() != null) confidence += 10;
        if (route.getOriginLatitude() != null && route.getDestinationLatitude() != null) confidence += 10;
        if (traffic != TrafficCondition.UNKNOWN) confidence += 10;
        confidence += Math.min(15, history.size() * 3.0);
        if (latestEvent == null) confidence -= 15;
        else if (staleHours > 12) confidence -= 12;
        else if (staleHours > 6) confidence -= 7;
        else if (staleHours > 3) confidence -= 3;

        return new EtaCalculation(
                predictedDeliveryTime,
                score(risk, 0, 10),
                score(confidence, 0, 100),
                String.join(" ", factors)
        );
    }

    private long resolveTravelMinutes(
            Route route,
            Shipment shipment,
            LocalDateTime calculatedAt,
            List<String> factors
    ) {
        if (route.getEstimatedTimeMinutes() != null && route.getEstimatedTimeMinutes() > 0) {
            factors.add("Route travel time of " + route.getEstimatedTimeMinutes() + " minutes is the baseline.");
            return route.getEstimatedTimeMinutes();
        }
        if (route.getDistanceKm() != null && route.getDistanceKm().signum() > 0) {
            long minutes = Math.max(1, route.getDistanceKm()
                    .multiply(BigDecimal.valueOf(1.5))
                    .setScale(0, RoundingMode.CEILING)
                    .longValue());
            factors.add("Travel time is estimated from " + route.getDistanceKm()
                    + " km at an explainable 40 km/h average speed.");
            return minutes;
        }
        long promisedWindow = Math.max(60, Duration.between(
                calculatedAt,
                shipment.getEstimatedDeliveryDate().atTime(LocalTime.MAX)
        ).toMinutes());
        factors.add("Route metrics are unavailable; the promised delivery window is used as a fallback.");
        return promisedWindow;
    }

    private double progressFactor(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> 1.0;
            case PICKED_UP -> 0.90;
            case IN_TRANSIT -> 0.55;
            case OUT_FOR_DELIVERY -> 0.15;
            case FAILED_DELIVERY -> 0.35;
            case DELIVERED, CANCELLED -> 0;
        };
    }

    private long handlingMinutes(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> 120;
            case PICKED_UP -> 45;
            case FAILED_DELIVERY -> 180;
            default -> 0;
        };
    }

    private double trafficMultiplier(TrafficCondition traffic) {
        return switch (traffic) {
            case LIGHT -> 1.0;
            case MODERATE -> 1.15;
            case HEAVY -> 1.35;
            case SEVERE -> 1.65;
            case UNKNOWN -> 1.10;
        };
    }

    private double trafficRisk(TrafficCondition traffic) {
        return switch (traffic) {
            case LIGHT -> 0.5;
            case MODERATE -> 1.5;
            case HEAVY -> 3.0;
            case SEVERE -> 5.0;
            case UNKNOWN -> 1.0;
        };
    }

    private double staleRisk(long staleHours, boolean missingHistory) {
        if (missingHistory || staleHours > 24) return 3.0;
        if (staleHours > 12) return 2.0;
        if (staleHours > 6) return 1.25;
        if (staleHours > 3) return 0.5;
        return 0;
    }

    private String formatMultiplier(double multiplier) {
        return BigDecimal.valueOf(multiplier).stripTrailingZeros().toPlainString() + "x";
    }

    private BigDecimal score(double value, double minimum, double maximum) {
        return BigDecimal.valueOf(Math.max(minimum, Math.min(maximum, value)))
                .setScale(1, RoundingMode.HALF_UP);
    }
}
