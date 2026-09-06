package com.shiptrack.shiptrack_pro.event;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateResponse;

public record DriverLocationUpdatedEvent(LocationUpdateResponse location) {
}
