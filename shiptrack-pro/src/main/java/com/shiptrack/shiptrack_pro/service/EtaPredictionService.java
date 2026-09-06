package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ETAPredictionResponse;

public interface EtaPredictionService {
    ETAPredictionResponse predict(Long shipmentId, String requesterEmail);
    ETAPredictionResponse getCurrentPrediction(Long shipmentId, String requesterEmail);
    void recalculateAutomatically(Long shipmentId);
}
