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
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ShipmentStatus.CREATED, EnumSet.of(ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED),
            ShipmentStatus.PICKED_UP, EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED),
            ShipmentStatus.IN_TRANSIT, EnumSet.of(
                    ShipmentStatus.OUT_FOR_DELIVERY,
                    ShipmentStatus.FAILED_DELIVERY,
                    ShipmentStatus.CANCELLED
            ),
            ShipmentStatus.OUT_FOR_DELIVERY, EnumSet.of(
                    ShipmentStatus.DELIVERED,
                    ShipmentStatus.FAILED_DELIVERY,
                    ShipmentStatus.CANCELLED
            ),
            ShipmentStatus.FAILED_DELIVERY, EnumSet.of(
                    ShipmentStatus.OUT_FOR_DELIVERY,
                    ShipmentStatus.CANCELLED
            ),
            ShipmentStatus.DELIVERED, EnumSet.noneOf(ShipmentStatus.class),
            ShipmentStatus.CANCELLED, EnumSet.noneOf(ShipmentStatus.class)
    );

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    @Override
    public ShipmentResponse createShipment(ShipmentRequest request, String creatorEmail) {
        User creator = userRepository.findByEmailIgnoreCase(creatorEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists"));

        Shipment shipment = Shipment.builder()
                .trackingNumber(generateTrackingNumber())
                .senderName(request.getSenderName().trim())
                .senderPhone(trimToNull(request.getSenderPhone()))
                .senderAddress(request.getSenderAddress().trim())
                .receiverName(request.getReceiverName().trim())
                .receiverPhone(trimToNull(request.getReceiverPhone()))
                .receiverEmail(request.getReceiverEmail().trim().toLowerCase(Locale.ROOT))
                .receiverAddress(request.getReceiverAddress().trim())
                .pickupAddress(request.getPickupAddress().trim())
                .deliveryAddress(request.getDeliveryAddress().trim())
                .priority(request.getPriority())
                .packageDescription(request.getPackageDescription().trim())
                .weightKg(request.getWeightKg())
                .dimensions(request.getDimensions().trim())
                .quantity(request.getQuantity())
                .declaredValue(request.getDeclaredValue())
                .fragile(request.getFragile())
                .status(ShipmentStatus.CREATED)
                .currentLocation(request.getPickupAddress().trim())
                .estimatedDeliveryDate(calculateEstimatedDeliveryDate(request.getPriority()))
                .createdBy(creator)
                .build();

        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {
        return mapToResponse(findShipment(id));
    }

    @Override
    public ShipmentResponse updateStatus(Long id, ShipmentStatusUpdateRequest request) {
        Shipment shipment = findShipment(id);
        ShipmentStatus currentStatus = shipment.getStatus();
        ShipmentStatus requestedStatus = request.getStatus();

        if (currentStatus == requestedStatus) {
            if (trimToNull(request.getCurrentLocation()) != null) {
                shipment.setCurrentLocation(request.getCurrentLocation().trim());
            }
            if (currentStatus == ShipmentStatus.CANCELLED
                    && trimToNull(request.getCancellationReason()) != null) {
                shipment.setCancellationReason(request.getCancellationReason().trim());
            }
            return mapToResponse(shipmentRepository.save(shipment));
        }

        if (!ALLOWED_TRANSITIONS.get(currentStatus).contains(requestedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid shipment status transition from " + currentStatus + " to " + requestedStatus
            );
        }

        String cancellationReason = requestedStatus == ShipmentStatus.CANCELLED
                ? requireCancellationReason(request.getCancellationReason())
                : null;
        shipment.setStatus(requestedStatus);
        if (trimToNull(request.getCurrentLocation()) != null) {
            shipment.setCurrentLocation(request.getCurrentLocation().trim());
        }
        if (requestedStatus == ShipmentStatus.CANCELLED) {
            shipment.setCancellationReason(cancellationReason);
        }
        if (requestedStatus == ShipmentStatus.DELIVERED) {
            shipment.setActualDeliveryDate(LocalDateTime.now());
            if (trimToNull(request.getCurrentLocation()) == null) {
                shipment.setCurrentLocation(shipment.getDeliveryAddress());
            }
        }
        return mapToResponse(shipmentRepository.save(shipment));
    }

    @Override
    public void cancelShipment(Long id, String cancellationReason) {
        Shipment shipment = findShipment(id);
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.get(shipment.getStatus()).contains(ShipmentStatus.CANCELLED)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Shipment in " + shipment.getStatus() + " status cannot be cancelled"
            );
        }
        String normalizedReason = requireCancellationReason(cancellationReason);
        shipment.setStatus(ShipmentStatus.CANCELLED);
        shipment.setCancellationReason(normalizedReason);
        shipmentRepository.save(shipment);
    }

    private Shipment findShipment(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment not found with id: " + id));
    }

    private String generateTrackingNumber() {
        return "SHP-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
    }

    private LocalDate calculateEstimatedDeliveryDate(ShipmentPriority priority) {
        int estimatedDays = priority == ShipmentPriority.EXPRESS ? 2 : 5;
        return LocalDate.now().plusDays(estimatedDays);
    }

    private String requireCancellationReason(String cancellationReason) {
        String normalizedReason = trimToNull(cancellationReason);
        if (normalizedReason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancellation reason is required");
        }
        if (normalizedReason.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancellation reason must not exceed 500 characters"
            );
        }
        return normalizedReason;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .senderName(shipment.getSenderName())
                .senderPhone(shipment.getSenderPhone())
                .senderAddress(shipment.getSenderAddress())
                .receiverName(shipment.getReceiverName())
                .receiverPhone(shipment.getReceiverPhone())
                .receiverEmail(shipment.getReceiverEmail())
                .receiverAddress(shipment.getReceiverAddress())
                .pickupAddress(shipment.getPickupAddress())
                .deliveryAddress(shipment.getDeliveryAddress())
                .priority(shipment.getPriority())
                .packageDescription(shipment.getPackageDescription())
                .weightKg(shipment.getWeightKg())
                .dimensions(shipment.getDimensions())
                .quantity(shipment.getQuantity())
                .declaredValue(shipment.getDeclaredValue())
                .fragile(shipment.getFragile())
                .status(shipment.getStatus())
                .currentLocation(shipment.getCurrentLocation())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .cancellationReason(shipment.getCancellationReason())
                .createdById(shipment.getCreatedBy().getId())
                .createdBy(shipment.getCreatedBy().getEmail())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
