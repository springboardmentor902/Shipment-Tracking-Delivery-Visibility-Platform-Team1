package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;
import com.shiptrack.shiptrack_pro.service.EtaPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eta")
@RequiredArgsConstructor
public class EtaController {

    private final EtaPredictionService etaPredictionService;

    @PostMapping("/{shipmentId}/predict")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ETAPredictionResponse> predict(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                etaPredictionService.predict(shipmentId, authentication.getName()));
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ETAPredictionResponse> getCurrentPrediction(
            @PathVariable Long shipmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                etaPredictionService.getCurrentPrediction(shipmentId, authentication.getName()));
    }
}
