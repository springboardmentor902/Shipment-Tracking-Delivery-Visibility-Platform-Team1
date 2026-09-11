package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    // CREATE ROUTE
    @PostMapping
    public ResponseEntity<Route> createRoute(
            @Valid @RequestBody RouteRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Route route = routeService.createRoute(
                request,
                userEmail
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(route);
    }

    // GET ROUTE BY SHIPMENT
    @GetMapping("/{shipmentId}")
    public ResponseEntity<Route> getRouteByShipment(
            @PathVariable Long shipmentId,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Route route = routeService.getRouteByShipment(
                shipmentId,
                userEmail
        );

        return ResponseEntity.ok(route);
    }

    // CHANGE / ASSIGN DRIVER
    @PutMapping("/{shipmentId}/driver/{driverId}")
    public ResponseEntity<Route> assignDriver(
            @PathVariable Long shipmentId,
            @PathVariable Long driverId,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Route route = routeService.assignDriver(
                shipmentId,
                driverId,
                userEmail
        );

        return ResponseEntity.ok(route);
    }
}