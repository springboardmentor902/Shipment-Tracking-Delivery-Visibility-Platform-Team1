package com.shiptrack.shiptrack_pro.controller;

import com.shiptrack.shiptrack_pro.dto.DriverLocationRequest;
import com.shiptrack.shiptrack_pro.dto.LocationUpdateResponse;
import com.shiptrack.shiptrack_pro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class TrackingController {

    private final RouteService routeService;

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<LocationUpdateResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody DriverLocationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(routeService.updateLocation(id, request, authentication.getName()));
    }
}
