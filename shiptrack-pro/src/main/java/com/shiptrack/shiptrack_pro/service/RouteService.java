package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.DriverAssignmentRequest;
import com.shiptrack.shiptrack_pro.dto.DriverLocationRequest;
import com.shiptrack.shiptrack_pro.dto.LocationUpdateResponse;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;

public interface RouteService {
    RouteResponse createRoute(RouteRequest request, String requesterEmail);
    RouteResponse getRouteByShipmentId(Long shipmentId, String requesterEmail);
    RouteResponse assignDriver(Long routeId, DriverAssignmentRequest request, String requesterEmail);
    LocationUpdateResponse updateLocation(Long routeId, DriverLocationRequest request, String requesterEmail);
}
