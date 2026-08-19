package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.OperatorAssignmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentResponse;
import com.shiptrack.shiptrack_pro.dto.ShipmentStatusUpdateRequest;

import java.util.List;

public interface ShipmentService {
    ShipmentResponse createShipment(ShipmentRequest request, String creatorEmail);
    List<ShipmentResponse> getAllShipments(String requesterEmail);
    ShipmentResponse getShipmentById(Long id, String requesterEmail);
    ShipmentResponse updateStatus(Long id, ShipmentStatusUpdateRequest request, String requesterEmail);
    ShipmentResponse assignOperator(Long id, OperatorAssignmentRequest request, String requesterEmail);
    void cancelShipment(Long id, String cancellationReason, String requesterEmail);
}
