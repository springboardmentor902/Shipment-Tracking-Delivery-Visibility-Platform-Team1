package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.event.DriverLocationUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TrackingLocationBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void broadcast(DriverLocationUpdatedEvent event) {
        messagingTemplate.convertAndSend(
                destinationFor(event.location().getShipmentId()),
                event.location());
    }

    public static String destinationFor(Long shipmentId) {
        return "/topic/shipments/" + shipmentId + "/location";
    }
}
