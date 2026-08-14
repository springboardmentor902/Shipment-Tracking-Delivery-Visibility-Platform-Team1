package com.shiptrack.shiptrack_pro.entity;

/**
 * The closed set of states a shipment can have during its lifecycle.
 */
public enum ShipmentStatus {
    CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_DELIVERY,
    CANCELLED
}
