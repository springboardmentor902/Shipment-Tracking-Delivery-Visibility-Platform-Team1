package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;

import java.math.BigDecimal;

@Data
public class ShipmentRequest {

    @NotBlank(message = "Sender name is required")
    @Size(max = 120, message = "Sender name must not exceed 120 characters")
    private String senderName;

    @Size(max = 25, message = "Sender phone must not exceed 25 characters")
    private String senderPhone;

    @NotBlank(message = "Sender address is required")
    @Size(max = 500, message = "Sender address must not exceed 500 characters")
    private String senderAddress;

    @NotBlank(message = "Receiver name is required")
    @Size(max = 120, message = "Receiver name must not exceed 120 characters")
    private String receiverName;

    @Size(max = 25, message = "Receiver phone must not exceed 25 characters")
    private String receiverPhone;

    @NotBlank(message = "Receiver email is required")
    @Email(message = "Receiver email must be valid")
    @Size(max = 254, message = "Receiver email must not exceed 254 characters")
    private String receiverEmail;

    @NotBlank(message = "Receiver address is required")
    @Size(max = 500, message = "Receiver address must not exceed 500 characters")
    private String receiverAddress;

    @NotBlank(message = "Pickup address is required")
    @Size(max = 500, message = "Pickup address must not exceed 500 characters")
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;

    @NotNull(message = "Priority is required")
    private ShipmentPriority priority;

    @NotBlank(message = "Package description is required")
    @Size(max = 500, message = "Package description must not exceed 500 characters")
    private String packageDescription;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be greater than zero")
    @Digits(integer = 8, fraction = 2, message = "Weight can have at most 8 integer and 2 decimal digits")
    private BigDecimal weightKg;

    @NotBlank(message = "Package dimensions are required")
    @Size(max = 100, message = "Package dimensions must not exceed 100 characters")
    private String dimensions;

    @NotNull(message = "Package quantity is required")
    @Min(value = 1, message = "Package quantity must be at least one")
    private Integer quantity;

    @NotNull(message = "Declared value is required")
    @DecimalMin(value = "0.00", message = "Declared value cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Declared value can have at most 12 integer and 2 decimal digits")
    private BigDecimal declaredValue;

    @NotNull(message = "Fragile flag is required")
    private Boolean fragile;
}
