package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEventType;

import java.math.BigDecimal;

public interface TrackingEventService {
    void record(
            Shipment shipment,
            TrackingEventType eventType,
            ShipmentStatus status,
            String location,
            BigDecimal latitude,
            BigDecimal longitude
    );
}
