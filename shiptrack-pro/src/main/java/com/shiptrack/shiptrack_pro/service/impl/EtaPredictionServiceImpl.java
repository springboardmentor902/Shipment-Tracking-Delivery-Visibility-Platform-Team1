package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;
import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.ETAPredictionRepository;
import com.shiptrack.shiptrack_pro.repository.RouteRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.event.DelayRiskThresholdCrossedEvent;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.EtaPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EtaPredictionServiceImpl implements EtaPredictionService {

    private final ETAPredictionRepository etaPredictionRepository;
    private final ShipmentRepository shipmentRepository;
    private final RouteRepository routeRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final UserRepository userRepository;
    private final EtaCalculationService calculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Value("${app.eta.at-risk-threshold:6.0}")
    private BigDecimal atRiskThreshold;

    @Override
    public ETAPredictionResponse predict(Long shipmentId, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        Shipment shipment = findShipment(shipmentId);
        requirePredictionManagement(shipment, requester);
        return calculateAndSave(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    public ETAPredictionResponse getCurrentPrediction(Long shipmentId, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        ETAPrediction prediction = etaPredictionRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ETA prediction not found for shipment id: " + shipmentId));
        requirePredictionVisibility(prediction.getShipment(), requester);
        return mapToResponse(prediction);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateAutomatically(Long shipmentId) {
        Shipment shipment = shipmentRepository.findOneById(shipmentId).orElse(null);
        if (shipment == null) {
            return;
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            etaPredictionRepository.findByShipmentId(shipmentId)
                    .ifPresent(etaPredictionRepository::delete);
            return;
        }
        if (!routeRepository.existsByShipmentId(shipmentId)) {
            return;
        }
        calculateAndSave(shipment);
    }

    private ETAPredictionResponse calculateAndSave(Shipment shipment) {
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cancelled shipment has no ETA");
        }
        Route route = routeRepository.findByShipmentId(shipment.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Route not found for shipment id: " + shipment.getId()));
        List<TrackingEvent> history = trackingEventRepository
                .findAllByShipmentIdOrderByRecordedAtAsc(shipment.getId());
        LocalDateTime calculatedAt = LocalDateTime.now(clock);
        EtaCalculation calculation = calculationService.calculate(
                shipment, route, history, calculatedAt);

        ETAPrediction prediction = etaPredictionRepository.findByShipmentId(shipment.getId())
                .orElseGet(() -> ETAPrediction.builder().shipment(shipment).build());
        BigDecimal previousRisk = prediction.getDelayRiskScore();
        prediction.setPredictedDeliveryTime(calculation.predictedDeliveryTime());
        prediction.setDelayRiskScore(calculation.delayRiskScore());
        prediction.setConfidenceScore(calculation.confidenceScore());
        prediction.setFactors(calculation.factors());
        prediction.setCalculatedAt(calculatedAt);
        ETAPrediction saved = etaPredictionRepository.save(prediction);
        boolean crossedThreshold = calculation.delayRiskScore().compareTo(atRiskThreshold) > 0
                && (previousRisk == null || previousRisk.compareTo(atRiskThreshold) <= 0);
        if (crossedThreshold) {
            eventPublisher.publishEvent(new DelayRiskThresholdCrossedEvent(shipment.getId()));
        }
        return mapToResponse(saved);
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

    private void requirePredictionVisibility(Shipment shipment, User requester) {
        boolean allowed = switch (parseRole(requester)) {
            case CUSTOMER, BUSINESS_CLIENT -> shipment.getCreatedBy().getId().equals(requester.getId());
            case LOGISTICS_OPERATOR -> shipment.getAssignedOperator() != null
                    && shipment.getAssignedOperator().getId().equals(requester.getId());
            case ADMINISTRATOR -> true;
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this ETA");
        }
    }

    private void requirePredictionManagement(Shipment shipment, User requester) {
        Role role = parseRole(requester);
        boolean allowed = role == Role.ADMINISTRATOR
                || (role == Role.LOGISTICS_OPERATOR
                && shipment.getAssignedOperator() != null
                && shipment.getAssignedOperator().getId().equals(requester.getId()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot recalculate this ETA");
        }
    }

    private ETAPredictionResponse mapToResponse(ETAPrediction prediction) {
        return ETAPredictionResponse.builder()
                .id(prediction.getId())
                .shipmentId(prediction.getShipment().getId())
                .trackingNumber(prediction.getShipment().getTrackingNumber())
                .predictedDeliveryTime(prediction.getPredictedDeliveryTime())
                .delayRiskScore(prediction.getDelayRiskScore())
                .confidenceScore(prediction.getConfidenceScore())
                .factors(prediction.getFactors())
                .calculatedAt(prediction.getCalculatedAt())
                .atRisk(prediction.getDelayRiskScore().compareTo(atRiskThreshold) > 0)
                .build();
    }
}
