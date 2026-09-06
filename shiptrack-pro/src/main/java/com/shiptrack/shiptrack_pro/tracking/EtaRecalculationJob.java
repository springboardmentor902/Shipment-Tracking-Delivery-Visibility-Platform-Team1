package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.service.EtaPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtaRecalculationJob {

    private static final List<ShipmentStatus> IN_PROGRESS_STATUSES = List.of(
            ShipmentStatus.CREATED,
            ShipmentStatus.PICKED_UP,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.FAILED_DELIVERY
    );

    private final ShipmentRepository shipmentRepository;
    private final EtaPredictionService etaPredictionService;

    @Scheduled(
            fixedDelayString = "${app.eta.recalculation-interval-ms:1200000}",
            initialDelayString = "${app.eta.recalculation-initial-delay-ms:1200000}"
    )
    public void recalculateInProgressShipments() {
        shipmentRepository.findAllByStatusIn(IN_PROGRESS_STATUSES)
                .stream()
                .map(Shipment::getId)
                .forEach(this::recalculateSafely);
    }

    private void recalculateSafely(Long shipmentId) {
        try {
            etaPredictionService.recalculateAutomatically(shipmentId);
        } catch (RuntimeException exception) {
            log.warn("Scheduled ETA recalculation failed for shipment {}", shipmentId, exception);
        }
    }
}
