package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShipmentStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ShipmentStatus status;

    @Size(max = 500, message = "Current location must not exceed 500 characters")
    private String currentLocation;

    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;
}
