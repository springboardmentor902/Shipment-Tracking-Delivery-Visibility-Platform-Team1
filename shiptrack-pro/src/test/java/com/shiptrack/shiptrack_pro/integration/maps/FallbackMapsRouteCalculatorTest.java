package com.shiptrack.shiptrack_pro.integration.maps;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FallbackMapsRouteCalculatorTest {

    @Test
    void usesGeoapifyWhenGoogleDoesNotReturnCompleteMetrics() {
        GoogleMapsRouteCalculator google = mock(GoogleMapsRouteCalculator.class);
        GeoapifyMapsRouteCalculator geoapify = mock(GeoapifyMapsRouteCalculator.class);
        RouteCalculation fallback = new RouteCalculation(
                new Coordinates(new BigDecimal("18.52"), new BigDecimal("73.85")),
                new Coordinates(new BigDecimal("12.97"), new BigDecimal("77.59")),
                new BigDecimal("840.25"),
                920L
        );
        when(google.calculate("Pune", "Bengaluru")).thenReturn(RouteCalculation.empty());
        when(geoapify.calculate("Pune", "Bengaluru")).thenReturn(fallback);

        RouteCalculation result = new FallbackMapsRouteCalculator(google, geoapify)
                .calculate("Pune", "Bengaluru");

        assertThat(result).isSameAs(fallback);
        verify(geoapify).calculate("Pune", "Bengaluru");
    }

    @Test
    void doesNotSpendFallbackCreditsWhenGoogleSucceeds() {
        GoogleMapsRouteCalculator google = mock(GoogleMapsRouteCalculator.class);
        GeoapifyMapsRouteCalculator geoapify = mock(GeoapifyMapsRouteCalculator.class);
        RouteCalculation googleResult = new RouteCalculation(
                new Coordinates(BigDecimal.ONE, BigDecimal.valueOf(2)),
                new Coordinates(BigDecimal.TEN, BigDecimal.ZERO),
                new BigDecimal("500"),
                600L
        );
        when(google.calculate("Pune", "Bengaluru")).thenReturn(googleResult);

        RouteCalculation result = new FallbackMapsRouteCalculator(google, geoapify)
                .calculate("Pune", "Bengaluru");

        assertThat(result).isSameAs(googleResult);
        verifyNoInteractions(geoapify);
    }
}
