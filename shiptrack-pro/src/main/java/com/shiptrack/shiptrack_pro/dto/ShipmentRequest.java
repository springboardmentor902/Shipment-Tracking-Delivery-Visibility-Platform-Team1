package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.shiptrack.shiptrack_pro.entity.ShipmentPriority;

import java.util.List;

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

    @NotEmpty(message = "At least one package is required")
    @Size(max = 50, message = "A shipment cannot contain more than 50 packages")
    private List<@Valid PackageRequest> packages;
}
