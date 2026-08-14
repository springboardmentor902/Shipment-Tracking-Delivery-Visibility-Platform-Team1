package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentStatusUpdateRequest;
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
                .role("BUSINESS_CLIENT")
                .status("ACTIVE")
                .build();
    }

    @Test
    void createShipmentStartsInCreatedStatus() {
        ShipmentRequest request = validRequest();
        when(userRepository.findByEmailIgnoreCase(creator.getEmail())).thenReturn(Optional.of(creator));
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
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void updateStatusAllowsExpectedLifecycleTransition() {
        Shipment shipment = existingShipment(ShipmentStatus.CREATED);
        ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest();
        request.setStatus(ShipmentStatus.PICKED_UP);
        request.setCurrentLocation("Pune sorting hub");
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        ShipmentResponse response = shipmentService.updateStatus(11L, request);

        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(response.getCurrentLocation()).isEqualTo("Pune sorting hub");
    }

    @Test
    void updateStatusRejectsSkippedLifecycleTransition() {
        Shipment shipment = existingShipment(ShipmentStatus.CREATED);
        ShipmentStatusUpdateRequest request = new ShipmentStatusUpdateRequest();
        request.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.updateStatus(11L, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(exception.getReason()).contains("CREATED", "DELIVERED");
                });
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void cancelShipmentUsesCancelledStatusInsteadOfDeletingAuditRecord() {
        Shipment shipment = existingShipment(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        shipmentService.cancelShipment(11L, "Customer requested cancellation");

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(shipment.getCancellationReason()).isEqualTo("Customer requested cancellation");
        verify(shipmentRepository).save(shipment);
        verify(shipmentRepository, never()).delete(any());
    }

    @Test
    void deliveredShipmentCannotBeCancelled() {
        Shipment shipment = existingShipment(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.cancelShipment(11L, "Customer request"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value()));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void missingShipmentReturnsNotFound() {
        when(shipmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getShipmentById(404L))
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
        request.setPackageDescription("Training material");
        request.setWeightKg(new BigDecimal("4.50"));
        request.setDimensions("40 x 30 x 20 cm");
        request.setQuantity(2);
        request.setDeclaredValue(new BigDecimal("2500.00"));
        request.setFragile(false);
        return request;
    }

    private Shipment existingShipment(ShipmentStatus status) {
        return Shipment.builder()
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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
