package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequest {

    @NotBlank(message = "Package description is required")
    @Size(max = 500, message = "Package description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Package weight is required")
    @DecimalMin(value = "0.01", message = "Package weight must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Package weight can have at most 8 integer and 2 decimal digits")
    private BigDecimal weightKg;

    @NotBlank(message = "Package dimensions are required")
    @Size(max = 100, message = "Package dimensions must not exceed 100 characters")
    private String dimensions;

    @NotNull(message = "Package quantity is required")
    @Min(value = 1, message = "Package quantity must be at least one")
    private Integer quantity;

    @NotNull(message = "Package declared value is required")
    @DecimalMin(value = "0.00", message = "Package declared value cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Package declared value can have at most 12 integer and 2 decimal digits")
    private BigDecimal declaredValue;

    @NotNull(message = "Package fragile flag is required")
    private Boolean fragile;
}
