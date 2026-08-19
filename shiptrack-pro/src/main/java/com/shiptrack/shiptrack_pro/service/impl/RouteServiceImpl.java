package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.DriverAssignmentRequest;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.integration.maps.MapsRouteCalculator;
import com.shiptrack.shiptrack_pro.integration.maps.RouteCalculation;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final MapsRouteCalculator mapsRouteCalculator;

    @Override
    public RouteResponse createRoute(RouteRequest request, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        Role role = parseRole(requester);
        if (role != Role.LOGISTICS_OPERATOR && role != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only a logistics operator or administrator can create routes");
        }

        Shipment shipment = findShipment(request.getShipmentId());
        if (routeRepository.existsByShipmentId(shipment.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A route already exists for shipment id: " + shipment.getId());
        }

        if (role == Role.LOGISTICS_OPERATOR) {
            if (shipment.getAssignedOperator() == null
                    || !shipment.getAssignedOperator().getId().equals(requester.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Only the assigned logistics operator can create this route");
            }
        }

        RouteCalculation calculation;
        try {
            calculation = mapsRouteCalculator.calculate(
                    shipment.getPickupAddress(), shipment.getDeliveryAddress());
        } catch (RuntimeException exception) {
            calculation = RouteCalculation.empty();
        }

        Route route = Route.builder()
                .shipment(shipment)
                .originAddress(shipment.getPickupAddress())
                .destinationAddress(shipment.getDeliveryAddress())
                .originLatitude(calculation.origin() == null ? null : calculation.origin().latitude())
                .originLongitude(calculation.origin() == null ? null : calculation.origin().longitude())
                .destinationLatitude(calculation.destination() == null ? null : calculation.destination().latitude())
                .destinationLongitude(calculation.destination() == null ? null : calculation.destination().longitude())
                .distanceKm(calculation.distanceKm())
                .estimatedTimeMinutes(calculation.estimatedTimeMinutes())
                .driverName(trimToNull(request.getDriverName()))
                .driverPhone(trimToNull(request.getDriverPhone()))
                .vehicleNumber(trimToNull(request.getVehicleNumber()))
                .createdBy(requester)
                .build();

        return mapToResponse(routeRepository.save(route));
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRouteByShipmentId(Long shipmentId, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        Route route = routeRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Route not found for shipment id: " + shipmentId));
        requireRouteVisibility(route, requester);
        return mapToResponse(route);
    }

    @Override
    public RouteResponse assignDriver(
            Long routeId,
            DriverAssignmentRequest request,
            String requesterEmail
    ) {
        User requester = requireUser(requesterEmail);
        Route route = routeRepository.findOneById(routeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Route not found with id: " + routeId));
        requireRouteManagement(route, requester);

        route.setDriverName(request.getDriverName().trim());
        route.setDriverPhone(trimToNull(request.getDriverPhone()));
        route.setVehicleNumber(trimToNull(request.getVehicleNumber()));
        return mapToResponse(routeRepository.save(route));
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findOneById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found with id: " + shipmentId));
    }

    private User requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));
    }

    private Role parseRole(User user) {
        try {
            return Role.valueOf(user.getRole());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has an unsupported role");
        }
    }

    private void requireRouteVisibility(Route route, User requester) {
        Shipment shipment = route.getShipment();
        boolean allowed = switch (parseRole(requester)) {
            case CUSTOMER, BUSINESS_CLIENT -> shipment.getCreatedBy().getId().equals(requester.getId());
            case LOGISTICS_OPERATOR -> shipment.getAssignedOperator() != null
                    && shipment.getAssignedOperator().getId().equals(requester.getId());
            case ADMINISTRATOR -> true;
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this route");
        }
    }

    private void requireRouteManagement(Route route, User requester) {
        Role role = parseRole(requester);
        boolean allowed = role == Role.ADMINISTRATOR
                || (role == Role.LOGISTICS_OPERATOR
                && route.getShipment().getAssignedOperator() != null
                && route.getShipment().getAssignedOperator().getId().equals(requester.getId()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this route");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RouteResponse mapToResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(route.getShipment().getId())
                .trackingNumber(route.getShipment().getTrackingNumber())
                .originAddress(route.getOriginAddress())
                .destinationAddress(route.getDestinationAddress())
                .originLatitude(route.getOriginLatitude())
                .originLongitude(route.getOriginLongitude())
                .destinationLatitude(route.getDestinationLatitude())
                .destinationLongitude(route.getDestinationLongitude())
                .distanceKm(route.getDistanceKm())
                .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                .driverName(route.getDriverName())
                .driverPhone(route.getDriverPhone())
                .vehicleNumber(route.getVehicleNumber())
                .createdById(route.getCreatedBy().getId())
                .createdBy(route.getCreatedBy().getEmail())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
