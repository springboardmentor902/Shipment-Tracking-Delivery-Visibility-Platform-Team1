package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RouteRequest {

    @NotNull
    private Long shipmentId;

    private Long driverId;

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    private Double distanceKm;

    private Integer estimatedMinutes;

    private String status;
}