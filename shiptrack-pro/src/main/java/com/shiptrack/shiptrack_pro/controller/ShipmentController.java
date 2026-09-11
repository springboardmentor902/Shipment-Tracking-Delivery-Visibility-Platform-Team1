package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // CREATE SHIPMENT
    @PostMapping
    public ResponseEntity<Shipment> createShipment(
            @Valid @RequestBody ShipmentRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Shipment shipment =
                shipmentService.createShipment(request, userEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(shipment);
    }

    // GET MY SHIPMENTS
    @GetMapping
    public ResponseEntity<List<Shipment>> getMyShipments(
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<Shipment> shipments =
                shipmentService.getMyShipments(userEmail);

        return ResponseEntity.ok(shipments);
    }

    // GET SHIPMENT DETAILS
    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Shipment shipment =
                shipmentService.getShipmentById(id, userEmail);

        return ResponseEntity.ok(shipment);
    }

    // UPDATE SHIPMENT
    @PutMapping("/{id}")
    public ResponseEntity<Shipment> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Shipment shipment =
                shipmentService.updateShipment(id, request, userEmail);

        return ResponseEntity.ok(shipment);
    }

    // CANCEL SHIPMENT
    @DeleteMapping("/{id}")
    public ResponseEntity<Shipment> cancelShipment(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        Shipment shipment =
                shipmentService.cancelShipment(id, userEmail);

        return ResponseEntity.ok(shipment);
    }
}