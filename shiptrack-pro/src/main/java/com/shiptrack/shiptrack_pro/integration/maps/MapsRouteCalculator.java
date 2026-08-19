package com.shiptrack.shiptrack_pro.integration.maps;

public interface MapsRouteCalculator {
    RouteCalculation calculate(String originAddress, String destinationAddress);
}
