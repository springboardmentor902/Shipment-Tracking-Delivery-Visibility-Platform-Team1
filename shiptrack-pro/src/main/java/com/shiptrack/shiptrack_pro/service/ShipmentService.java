package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentStatusUpdateRequest;

import java.util.List;

public interface ShipmentService {
    ShipmentResponse createShipment(ShipmentRequest request, String creatorEmail);
    List<ShipmentResponse> getAllShipments();
    ShipmentResponse getShipmentById(Long id);
    ShipmentResponse updateStatus(Long id, ShipmentStatusUpdateRequest request);
    void cancelShipment(Long id, String cancellationReason);
}
