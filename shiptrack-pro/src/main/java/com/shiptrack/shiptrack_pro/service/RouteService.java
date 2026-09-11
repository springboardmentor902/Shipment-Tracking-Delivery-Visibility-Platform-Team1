package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final OpenRouteService openRouteService;

    // CREATE ROUTE
    public Route createRoute(RouteRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkOperatorOrAdmin(user);

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (routeRepository.existsByShipment(shipment)) {
            throw new RuntimeException(
                    "Route already exists for this shipment"
            );
        }

        Route route = new Route();

        route.setShipment(shipment);
        route.setOrigin(request.getOrigin());
        route.setDestination(request.getDestination());

        route.setStatus(
                request.getStatus() != null
                        ? request.getStatus()
                        : "PLANNED"
        );

        /*
         * OpenRouteService:
         * Address -> Coordinates
         * Coordinates -> Distance + ETA
         */
        try {

            double[] originCoordinates =
                    openRouteService.geocode(request.getOrigin());

            double[] destinationCoordinates =
                    openRouteService.geocode(request.getDestination());

            OpenRouteService.RouteDetails routeDetails =
                    openRouteService.getRouteDetails(
                            originCoordinates[0],
                            originCoordinates[1],
                            destinationCoordinates[0],
                            destinationCoordinates[1]
                    );

            route.setDistanceKm(
                    routeDetails.distanceKm()
            );

            route.setEstimatedMinutes(
                    routeDetails.estimatedMinutes()
            );

        } catch (Exception e) {

            // Route should still be saved if map service fails
            route.setDistanceKm(null);
            route.setEstimatedMinutes(null);
        }

        // OPTIONAL DRIVER ASSIGNMENT
        if (request.getDriverId() != null) {

            User driver = userRepository.findById(
                    request.getDriverId()
            ).orElseThrow(() ->
                    new RuntimeException("Driver not found")
            );

            if (!"LOGISTICS_OPERATOR".equals(driver.getRole())) {
                throw new RuntimeException(
                        "Selected user is not a Logistics Operator"
                );
            }

            route.setDriver(driver);
        }

        return routeRepository.save(route);
    }

    // GET ROUTE BY SHIPMENT
    public Route getRouteByShipment(
            Long shipmentId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkOperatorOrAdmin(user);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        return routeRepository.findByShipment(shipment)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Route not found for this shipment"
                        )
                );
    }

    // CHANGE / ASSIGN DRIVER
    public Route assignDriver(
            Long shipmentId,
            Long driverId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkOperatorOrAdmin(user);

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        Route route = routeRepository.findByShipment(shipment)
                .orElseThrow(() ->
                        new RuntimeException("Route not found")
                );

        User driver = userRepository.findById(driverId)
                .orElseThrow(() ->
                        new RuntimeException("Driver not found")
                );

        if (!"LOGISTICS_OPERATOR".equals(driver.getRole())) {
            throw new RuntimeException(
                    "Selected user is not a Logistics Operator"
            );
        }

        route.setDriver(driver);

        return routeRepository.save(route);
    }

    // AUTHORIZATION
    private void checkOperatorOrAdmin(User user) {

        String role = user.getRole();

        if (!"LOGISTICS_OPERATOR".equals(role)
                && !"ADMIN".equals(role)) {

            throw new RuntimeException(
                    "Only Logistics Operator or Admin can manage routes"
            );
        }
    }
}