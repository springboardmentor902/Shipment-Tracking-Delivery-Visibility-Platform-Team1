package com.shiptrack.shiptrack_pro.tracking;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.service.EtaPredictionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class EtaRecalculationJobTest {

    @Test
    void scheduledJobRecalculatesEveryInProgressShipment() {
        Shipment first = Shipment.builder().id(11L).status(ShipmentStatus.IN_TRANSIT).build();
        Shipment second = Shipment.builder().id(12L).status(ShipmentStatus.OUT_FOR_DELIVERY).build();
        ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
        EtaPredictionService etaPredictionService = mock(EtaPredictionService.class);
        when(shipmentRepository.findAllByStatusIn(anyList())).thenReturn(List.of(first, second));

        new EtaRecalculationJob(shipmentRepository, etaPredictionService)
                .recalculateInProgressShipments();

        verify(etaPredictionService).recalculateAutomatically(11L);
        verify(etaPredictionService).recalculateAutomatically(12L);
        verifyNoMoreInteractions(etaPredictionService);
    }
}
