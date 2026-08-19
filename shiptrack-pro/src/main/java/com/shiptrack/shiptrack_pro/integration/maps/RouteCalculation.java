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
}
