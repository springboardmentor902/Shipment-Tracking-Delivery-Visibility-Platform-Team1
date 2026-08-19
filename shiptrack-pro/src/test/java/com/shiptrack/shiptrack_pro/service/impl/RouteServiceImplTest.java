package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.DriverAssignmentRequest;
import com.shiptrack.shiptrack_pro.dto.RouteRequest;
import com.shiptrack.shiptrack_pro.dto.RouteResponse;
import com.shiptrack.shiptrack_pro.entity.Route;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.integration.maps.Coordinates;
import com.shiptrack.shiptrack_pro.integration.maps.MapsRouteCalculator;
import com.shiptrack.shiptrack_pro.integration.maps.RouteCalculation;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MapsRouteCalculator mapsRouteCalculator;

    private RouteServiceImpl routeService;
    private User operator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        routeService = new RouteServiceImpl(
                routeRepository, shipmentRepository, userRepository, mapsRouteCalculator);
        operator = User.builder()
                .id(7L)
                .fullName("Logistics Operator")
                .email("operator@example.com")
                .role("LOGISTICS_OPERATOR")
                .status("ACTIVE")
                .build();
        shipment = Shipment.builder()
                .id(11L)
                .trackingNumber("SHP-ABCDEF123456")
                .pickupAddress("Pune, Maharashtra")
                .deliveryAddress("Bengaluru, Karnataka")
                .assignedOperator(operator)
                .build();
    }

    @Test
    void createRouteStoresGoogleCoordinatesDistanceAndTime() {
        RouteRequest request = new RouteRequest();
        request.setShipmentId(shipment.getId());
        request.setDriverName("Amit Kumar");

        when(userRepository.findByEmailIgnoreCase(operator.getEmail())).thenReturn(Optional.of(operator));
        when(shipmentRepository.findOneById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(routeRepository.existsByShipmentId(shipment.getId())).thenReturn(false);
        when(mapsRouteCalculator.calculate(shipment.getPickupAddress(), shipment.getDeliveryAddress()))
                .thenReturn(new RouteCalculation(
                        new Coordinates(new BigDecimal("18.5204303"), new BigDecimal("73.8567437")),
                        new Coordinates(new BigDecimal("12.9715987"), new BigDecimal("77.5945660")),
                        new BigDecimal("841.42"),
                        930L));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(31L);
            return route;
        });

        RouteResponse response = routeService.createRoute(request, operator.getEmail());

        assertThat(response.getId()).isEqualTo(31L);
        assertThat(response.getOriginLatitude()).isEqualByComparingTo("18.5204303");
        assertThat(response.getDestinationLongitude()).isEqualByComparingTo("77.5945660");
        assertThat(response.getDistanceKm()).isEqualByComparingTo("841.42");
        assertThat(response.getEstimatedTimeMinutes()).isEqualTo(930L);
        assertThat(response.getDriverName()).isEqualTo("Amit Kumar");
    }

    @Test
    void createRouteStillSavesWhenGoogleMapsFails() {
        RouteRequest request = new RouteRequest();
        request.setShipmentId(shipment.getId());

        when(userRepository.findByEmailIgnoreCase(operator.getEmail())).thenReturn(Optional.of(operator));
        when(shipmentRepository.findOneById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(routeRepository.existsByShipmentId(shipment.getId())).thenReturn(false);
        when(mapsRouteCalculator.calculate(shipment.getPickupAddress(), shipment.getDeliveryAddress()))
                .thenThrow(new IllegalStateException("Google unavailable"));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(32L);
            return route;
        });

        RouteResponse response = routeService.createRoute(request, operator.getEmail());

        assertThat(response.getId()).isEqualTo(32L);
        assertThat(response.getOriginLatitude()).isNull();
        assertThat(response.getDistanceKm()).isNull();
        assertThat(response.getEstimatedTimeMinutes()).isNull();
    }

    @Test
    void assignedOperatorCanChangeDriver() {
        Route route = Route.builder()
                .id(31L)
                .shipment(shipment)
                .originAddress(shipment.getPickupAddress())
                .destinationAddress(shipment.getDeliveryAddress())
                .createdBy(operator)
                .driverName("Old Driver")
                .build();
        DriverAssignmentRequest request = new DriverAssignmentRequest();
        request.setDriverName("New Driver");
        request.setDriverPhone("+91 98765 43210");
        request.setVehicleNumber("MH12AB1234");

        when(userRepository.findByEmailIgnoreCase(operator.getEmail())).thenReturn(Optional.of(operator));
        when(routeRepository.findOneById(route.getId())).thenReturn(Optional.of(route));
        when(routeRepository.save(route)).thenReturn(route);

        RouteResponse response = routeService.assignDriver(route.getId(), request, operator.getEmail());

        assertThat(response.getDriverName()).isEqualTo("New Driver");
        assertThat(response.getDriverPhone()).isEqualTo("+91 98765 43210");
        assertThat(response.getVehicleNumber()).isEqualTo("MH12AB1234");
    }

    @Test
    void operatorCannotCreateRouteForUnassignedShipment() {
        shipment.setAssignedOperator(null);
        RouteRequest request = new RouteRequest();
        request.setShipmentId(shipment.getId());

        when(userRepository.findByEmailIgnoreCase(operator.getEmail())).thenReturn(Optional.of(operator));
        when(shipmentRepository.findOneById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(routeRepository.existsByShipmentId(shipment.getId())).thenReturn(false);

        assertThatThrownBy(() -> routeService.createRoute(request, operator.getEmail()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value())
                                .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }
}
