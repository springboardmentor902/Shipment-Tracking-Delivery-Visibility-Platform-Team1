package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ShipmentRequest {

    @NotBlank
    private String senderName;

    @NotBlank
    private String receiverName;

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    @Valid
    private List<PackageRequest> packages = new ArrayList<>();
}