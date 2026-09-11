package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PackageRequest {

    @NotBlank
    private String description;

    @NotNull
    @Positive
    private Double weight;

    @NotNull
    @Positive
    private Integer quantity;
}
