package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.dto.ProofOfDeliveryResponse;
import com.shiptrack.shiptrack_pro.dto.ProofVerificationRequest;
import com.shiptrack.shiptrack_pro.entity.*;
import com.shiptrack.shiptrack_pro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import com.shiptrack.shiptrack_pro.security.Role;
import com.shiptrack.shiptrack_pro.service.FileStorageService;
import com.shiptrack.shiptrack_pro.service.ProofOfDeliveryService;
import com.shiptrack.shiptrack_pro.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProofOfDeliveryServiceImpl implements ProofOfDeliveryService {

    private final ProofOfDeliveryRepository proofRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final TrackingEventService trackingEventService;
    private final Clock clock;

    @Override
    public ProofOfDeliveryResponse submit(
            Long shipmentId,
            String recipientName,
            String deliveryNotes,
            MultipartFile signature,
            MultipartFile photo,
            String requesterEmail
    ) {
        User operator = requireUser(requesterEmail);
        Shipment shipment = findShipment(shipmentId);
        requireAssignedOperator(shipment, operator);
        if (shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Proof can only be submitted when the shipment is out for delivery"
            );
        }
        if (proofRepository.existsByShipmentId(shipmentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Proof of delivery already exists");
        }
        String normalizedRecipient = requiredText(
                recipientName,
                "Recipient name is required",
                120,
                "Recipient name must not exceed 120 characters"
        );
        String normalizedNotes = optionalText(deliveryNotes, 1000, "Delivery notes must not exceed 1000 characters");

        String signatureUrl = null;
        String photoUrl = null;
        try {
            signatureUrl = fileStorageService.store(signature);
            photoUrl = fileStorageService.store(photo);
            LocalDateTime deliveredAt = LocalDateTime.now(clock);

            ProofOfDelivery proof = ProofOfDelivery.builder()
                    .shipment(shipment)
                    .recipientName(normalizedRecipient)
                    .deliveryNotes(normalizedNotes)
                    .signatureUrl(signatureUrl)
                    .photoUrl(photoUrl)
                    .submittedBy(operator)
                    .submittedAt(deliveredAt)
                    .verificationStatus(ProofVerificationStatus.PENDING)
                    .build();
            ProofOfDelivery savedProof = proofRepository.saveAndFlush(proof);

            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setActualDeliveryDate(deliveredAt);
            shipment.setCurrentLocation(shipment.getDeliveryAddress());
            shipmentRepository.saveAndFlush(shipment);
            trackingEventService.record(
                    shipment,
                    TrackingEventType.STATUS_CHANGED,
                    ShipmentStatus.DELIVERED,
                    shipment.getCurrentLocation(),
                    null,
                    null
            );
            return map(savedProof);
        } catch (RuntimeException exception) {
            fileStorageService.delete(signatureUrl);
            fileStorageService.delete(photoUrl);
            throw exception;
        }
    }

    @Override
    public ProofOfDeliveryResponse verify(
            Long shipmentId,
            ProofVerificationRequest request,
            String requesterEmail
    ) {
        User verifier = requireUser(requesterEmail);
        Role verifierRole = parseRole(verifier);
        if (verifierRole != Role.SUPPORT_AGENT && verifierRole != Role.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a support agent or administrator can verify proof");
        }
        if (request.getStatus() != ProofVerificationStatus.VERIFIED
                && request.getStatus() != ProofVerificationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be VERIFIED or REJECTED");
        }

        ProofOfDelivery proof = findProof(shipmentId);
        proof.setVerificationStatus(request.getStatus());
        proof.setVerifiedBy(verifier);
        proof.setVerifiedAt(LocalDateTime.now(clock));
        proof.setVerificationNotes(optionalText(
                request.getNotes(), 500, "Verification notes must not exceed 500 characters"));
        return map(proofRepository.save(proof));
    }

    @Override
    @Transactional(readOnly = true)
    public ProofOfDeliveryResponse getByShipmentId(Long shipmentId, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        ProofOfDelivery proof = findProof(shipmentId);
        requireVisibility(proof.getShipment(), requester);
        return map(proof);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getStoredFile(String fileName, String requesterEmail) {
        User requester = requireUser(requesterEmail);
        String storedUrl = "/api/files/" + fileName;
        ProofOfDelivery proof = proofRepository.findBySignatureUrlOrPhotoUrl(storedUrl, storedUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        requireVisibility(proof.getShipment(), requester);
        return fileStorageService.load(fileName);
    }

    private ProofOfDelivery findProof(Long shipmentId) {
        return proofRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proof of delivery not found for shipment id: " + shipmentId));
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

    private void requireAssignedOperator(Shipment shipment, User requester) {
        if (parseRole(requester) != Role.LOGISTICS_OPERATOR
                || shipment.getAssignedOperator() == null
                || !shipment.getAssignedOperator().getId().equals(requester.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the assigned logistics operator can submit proof");
        }
    }

    private void requireVisibility(Shipment shipment, User requester) {
        boolean allowed = switch (parseRole(requester)) {
            case CUSTOMER, BUSINESS_CLIENT -> shipment.getCreatedBy().getId().equals(requester.getId());
            case LOGISTICS_OPERATOR -> shipment.getAssignedOperator() != null
                    && shipment.getAssignedOperator().getId().equals(requester.getId());
            case SUPPORT_AGENT, ADMINISTRATOR -> true;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this proof");
        }
    }

    private Role parseRole(User user) {
        try {
            return Role.valueOf(user.getRole());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has an unsupported role");
        }
    }

    private String requiredText(String value, String blankMessage, int maxLength, String tooLongMessage) {
        String normalized = optionalText(value, maxLength, tooLongMessage);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, blankMessage);
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, tooLongMessage);
        }
        return normalized;
    }

    private ProofOfDeliveryResponse map(ProofOfDelivery proof) {
        return ProofOfDeliveryResponse.builder()
                .id(proof.getId())
                .shipmentId(proof.getShipment().getId())
                .trackingNumber(proof.getShipment().getTrackingNumber())
                .recipientName(proof.getRecipientName())
                .deliveryNotes(proof.getDeliveryNotes())
                .signatureUrl(proof.getSignatureUrl())
                .photoUrl(proof.getPhotoUrl())
                .submittedById(proof.getSubmittedBy().getId())
                .submittedBy(proof.getSubmittedBy().getEmail())
                .submittedAt(proof.getSubmittedAt())
                .verificationStatus(proof.getVerificationStatus())
                .verifiedById(proof.getVerifiedBy() == null ? null : proof.getVerifiedBy().getId())
                .verifiedBy(proof.getVerifiedBy() == null ? null : proof.getVerifiedBy().getEmail())
                .verifiedAt(proof.getVerifiedAt())
                .verificationNotes(proof.getVerificationNotes())
                .build();
    }
}
