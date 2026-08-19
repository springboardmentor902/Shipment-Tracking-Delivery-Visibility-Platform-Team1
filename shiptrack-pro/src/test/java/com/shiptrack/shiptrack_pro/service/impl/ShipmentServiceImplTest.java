package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentStatusUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.PackageRequest;
import com.shiptrack.shiptrack_pro.entity.Package;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.User;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private UserRepository userRepository;

    private ShipmentServiceImpl shipmentService;
    private User creator;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentServiceImpl(shipmentRepository, userRepository);
        creator = User.builder()
                .id(7L)
                .fullName("Business User")
                .email("business@shiptrack.com")
                .role("LOGISTICS_OPERATOR")
                .status("ACTIVE")
                .build();
        lenient().when(userRepository.findByEmailIgnoreCase(creator.getEmail()))
                .thenReturn(Optional.of(creator));
    }

    @Test
    void createShipmentStartsInCreatedStatus() {
        ShipmentRequest request = validRequest();
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(11L);
            shipment.setCreatedAt(LocalDateTime.now());
            shipment.setUpdatedAt(LocalDateTime.now());
            return shipment;
        });

        ShipmentResponse response = shipmentService.createShipment(request, creator.getEmail());

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getTrackingNumber()).startsWith("SHP-").hasSize(16);
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(response.getCurrentLocation()).isEqualTo(request.getPickupAddress());
        assertThat(response.getEstimatedDeliveryDate()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(response.getCreatedBy()).isEqualTo(creator.getEmail());
        assertThat(response.getPackages()).hasSize(2);
        assertThat(response.getPackages().get(0).getDescription()).isEqualTo("Training material");
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void updateStatusAllowsExpectedLifecycleTransition() {
        Shipment shipment = existingShipment(ShipmentStatus.CREATED);
        ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest();
        request.setStatus(ShipmentStatus.PICKED_UP);
        request.setCurrentLocation("Pune sorting hub");
        when(shipmentRepository.findOneById(11L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        ShipmentResponse response = shipmentService.updateStatus(11L, request, creator.getEmail());

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(response.getCurrentLocation()).isEqualTo("Pune sorting hub");
    }

    @Test
    void updateStatusRejectsSkippedLifecycleTransition() {
        Shipment shipment = existingShipment(ShipmentStatus.CREATED);
        ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest();
        request.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findOneById(11L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.updateStatus(11L, request, creator.getEmail()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(exception.getReason()).contains("CREATED", "DELIVERED");
                });
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void cancelShipmentUsesCancelledStatusInsteadOfDeletingAuditRecord() {
        Shipment shipment = existingShipment(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findOneById(11L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        shipmentService.cancelShipment(11L, "Customer requested cancellation", creator.getEmail());

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(shipment.getCancellationReason()).isEqualTo("Customer requested cancellation");
        verify(shipmentRepository).save(shipment);
        verify(shipmentRepository, never()).delete(any());
    }

    @Test
    void deliveredShipmentCannotBeCancelled() {
        Shipment shipment = existingShipment(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findOneById(11L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.cancelShipment(
                11L, "Customer request", creator.getEmail()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value()));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void missingShipmentReturnsNotFound() {
        when(shipmentRepository.findOneById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getShipmentById(404L, creator.getEmail()))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    private ShipmentRequest validRequest() {
        ShipmentRequest request = new ShipmentRequest();
        request.setSenderName("Infosys Springboard");
        request.setSenderPhone("+91 99999 11111");
        request.setSenderAddress("Hinjewadi Phase 1, Pune");
        request.setReceiverName("Training Centre");
        request.setReceiverPhone("+91 99999 22222");
        request.setReceiverEmail("training@example.com");
        request.setReceiverAddress("Electronic City, Bengaluru");
        request.setPickupAddress("Pune, Maharashtra");
        request.setDeliveryAddress("Bengaluru, Karnataka");
        request.setPriority(ShipmentPriority.EXPRESS);
        PackageRequest firstPackage = packageRequest("Training material", "4.50", 2, false);
        PackageRequest secondPackage = packageRequest("Printed documents", "1.25", 1, true);
        request.setPackages(List.of(firstPackage, secondPackage));
        return request;
    }

    private PackageRequest packageRequest(String description, String weight, int quantity, boolean fragile) {
        PackageRequest request = new PackageRequest();
        request.setDescription(description);
        request.setWeightKg(new BigDecimal(weight));
        request.setDimensions("40 x 30 x 20 cm");
        request.setQuantity(quantity);
        request.setDeclaredValue(new BigDecimal("2500.00"));
        request.setFragile(fragile);
        return request;
    }

    private Shipment existingShipment(ShipmentStatus status) {
        Shipment shipment = Shipment.builder()
                .id(11L)
                .trackingNumber("SHP-ABCDEF123456")
                .senderName("Infosys Springboard")
                .senderAddress("Hinjewadi Phase 1, Pune")
                .receiverName("Training Centre")
                .receiverEmail("training@example.com")
                .receiverAddress("Electronic City, Bengaluru")
                .pickupAddress("Pune, Maharashtra")
                .deliveryAddress("Bengaluru, Karnataka")
                .priority(ShipmentPriority.EXPRESS)
                .packageDescription("Training material")
                .weightKg(new BigDecimal("4.50"))
                .dimensions("40 x 30 x 20 cm")
                .quantity(2)
                .declaredValue(new BigDecimal("2500.00"))
                .fragile(false)
                .status(status)
                .currentLocation("Pune, Maharashtra")
                .estimatedDeliveryDate(LocalDate.now().plusDays(2))
                .createdBy(creator)
                .assignedOperator(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        shipment.addPackage(Package.builder()
                .id(21L)
                .description("Training material")
                .weightKg(new BigDecimal("4.50"))
                .dimensions("40 x 30 x 20 cm")
                .quantity(2)
                .declaredValue(new BigDecimal("2500.00"))
                .fragile(false)
                .build());
        return shipment;
    }
}
