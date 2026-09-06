package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.event.TrackingEventAddedEvent;
import com.shiptrack.shiptrack_pro.service.EtaPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtaTrackingEventListener {

    private final EtaPredictionService etaPredictionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void recalculateAfterTrackingEvent(TrackingEventAddedEvent event) {
        try {
            etaPredictionService.recalculateAutomatically(event.shipmentId());
        } catch (RuntimeException exception) {
            log.warn("Automatic ETA recalculation failed for shipment {}", event.shipmentId(), exception);
        }
    }
}
