package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RouteRequest {

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;

    @Size(max = 120, message = "Driver name must not exceed 120 characters")
    private String driverName;

    @Size(max = 25, message = "Driver phone must not exceed 25 characters")
    private String driverPhone;

    @Size(max = 40, message = "Vehicle number must not exceed 40 characters")
    private String vehicleNumber;
}
