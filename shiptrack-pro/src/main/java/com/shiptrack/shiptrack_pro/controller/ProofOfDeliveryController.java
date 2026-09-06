package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ProofOfDeliveryResponse;
import com.shiptrack.shiptrack_pro.dto.ProofVerificationRequest;
import com.shiptrack.shiptrack_pro.service.ProofOfDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/pod")
@RequiredArgsConstructor
public class ProofOfDeliveryController {

    private final ProofOfDeliveryService proofOfDeliveryService;

    @PostMapping(value = "/{shipmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('LOGISTICS_OPERATOR')")
    public ResponseEntity<ProofOfDeliveryResponse> submit(
            @PathVariable Long shipmentId,
            @RequestParam("recipientName") String recipientName,
            @RequestParam(value = "deliveryNotes", required = false) String deliveryNotes,
            @RequestPart("signature") MultipartFile signature,
            @RequestPart("photo") MultipartFile photo,
            Authentication authentication
    ) {
        ProofOfDeliveryResponse response = proofOfDeliveryService.submit(
                shipmentId,
                recipientName,
                deliveryNotes,
                signature,
                photo,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{shipmentId}/verify")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<ProofOfDeliveryResponse> verify(
            @PathVariable Long shipmentId,
            @Valid @RequestBody ProofVerificationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                proofOfDeliveryService.verify(shipmentId, request, authentication.getName()));
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<ProofOfDeliveryResponse> get(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                proofOfDeliveryService.getByShipmentId(shipmentId, authentication.getName()));
    }
}
