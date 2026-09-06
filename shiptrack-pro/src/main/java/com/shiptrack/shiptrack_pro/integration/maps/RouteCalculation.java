package com.shiptrack.shiptrack_pro.integration.maps;

import java.math.BigDecimal;

public record RouteCalculation(
        Coordinates origin,
        Coordinates destination,
        BigDecimal distanceKm,
        Long estimatedTimeMinutes
) {
    public static RouteCalculation empty() {
        return new RouteCalculation(null, null, null, null);
    }

    public boolean hasCompleteMetrics() {
        return origin != null && destination != null
                && distanceKm != null && estimatedTimeMinutes != null;
    }

    public int dataPointCount() {
        int count = 0;
        if (origin != null) count++;
        if (destination != null) count++;
        if (distanceKm != null) count++;
        if (estimatedTimeMinutes != null) count++;
        return count;
    }
}
