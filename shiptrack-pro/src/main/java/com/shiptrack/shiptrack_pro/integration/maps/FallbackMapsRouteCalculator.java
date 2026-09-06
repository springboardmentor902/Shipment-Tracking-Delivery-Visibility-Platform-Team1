package com.shiptrack.shiptrack_pro.integration.maps;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class FallbackMapsRouteCalculator implements MapsRouteCalculator {

    private final GoogleMapsRouteCalculator googleMapsRouteCalculator;
    private final GeoapifyMapsRouteCalculator geoapifyMapsRouteCalculator;

    @Override
    public RouteCalculation calculate(String originAddress, String destinationAddress) {
        RouteCalculation googleResult = googleMapsRouteCalculator.calculate(originAddress, destinationAddress);
        if (googleResult.hasCompleteMetrics()) {
            return googleResult;
        }

        RouteCalculation geoapifyResult = geoapifyMapsRouteCalculator.calculate(originAddress, destinationAddress);
        if (geoapifyResult.hasCompleteMetrics() || geoapifyResult.dataPointCount() > googleResult.dataPointCount()) {
            return geoapifyResult;
        }
        return googleResult;
    }
}
