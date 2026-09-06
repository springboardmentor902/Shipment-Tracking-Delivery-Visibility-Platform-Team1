package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.dto.LocationUpdateResponse;
import com.shiptrack.shiptrack_pro.event.DriverLocationUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrackingLocationBroadcasterTest {

    @Test
    void locationIsBroadcastToItsShipmentTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        TrackingLocationBroadcaster broadcaster = new TrackingLocationBroadcaster(messagingTemplate);
        LocationUpdateResponse location = LocationUpdateResponse.builder()
                .routeId(7L)
                .shipmentId(42L)
                .trackingNumber("SHP-LIVE123456")
                .build();

        broadcaster.broadcast(new DriverLocationUpdatedEvent(location));

        verify(messagingTemplate).convertAndSend("/topic/shipments/42/location", location);
    }
}
