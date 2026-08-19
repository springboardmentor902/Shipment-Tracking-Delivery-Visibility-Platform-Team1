package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentStatusUpdateRequest;
import com.shiptrack.shiptrack_pro.dto.OperatorAssignmentRequest;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentRequest request,
            Authentication authentication
    ) {
        ShipmentResponse response = shipmentService.createShipment(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<List<ShipmentResponse>> getAllShipments(Authentication authentication) {
        return ResponseEntity.ok(shipmentService.getAllShipments(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> getShipmentById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id, authentication.getName()));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentStatusUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/operator")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> assignOperator(
            @PathVariable Long id,
            @Valid @RequestBody OperatorAssignmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(shipmentService.assignOperator(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<Void> cancelShipment(
            @PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication
    ) {
        shipmentService.cancelShipment(id, reason, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
