package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.entity.NotificationType;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.event.DelayRiskThresholdCrossedEvent;
import com.shiptrack.shiptrack_pro.event.TrackingEventAddedEvent;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTriggerListener {

    private final ShipmentRepository shipmentRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notifyShipmentOwner(TrackingEventAddedEvent event) {
        send(event.shipmentId(), NotificationType.SHIPMENT_UPDATE);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void warnShipmentOwner(DelayRiskThresholdCrossedEvent event) {
        send(event.shipmentId(), NotificationType.DELAY_WARNING);
    }

    private void send(Long shipmentId, NotificationType type) {
        try {
            Shipment shipment = shipmentRepository.findOneById(shipmentId).orElse(null);
            if (shipment != null) {
                notificationService.send(type, shipment.getCreatedBy(), shipment);
            }
        } catch (RuntimeException exception) {
            log.warn("Could not create {} notification for shipment {}", type, shipmentId, exception);
        }
    }
}
