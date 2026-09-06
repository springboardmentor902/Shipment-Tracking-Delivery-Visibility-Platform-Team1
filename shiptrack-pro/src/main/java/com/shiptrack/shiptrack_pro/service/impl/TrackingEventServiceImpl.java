package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.ShipmentStatus;
import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import com.shiptrack.shiptrack_pro.entity.TrackingEventType;
import com.shiptrack.shiptrack_pro.event.TrackingEventAddedEvent;
import com.shiptrack.shiptrack_pro.repository.TrackingEventRepository;
import com.shiptrack.shiptrack_pro.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TrackingEventServiceImpl implements TrackingEventService {

    private final TrackingEventRepository trackingEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void record(
            Shipment shipment,
            TrackingEventType eventType,
            ShipmentStatus status,
            String location,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        TrackingEvent trackingEvent = TrackingEvent.builder()
                .shipment(shipment)
                .eventType(eventType)
                .status(status)
                .location(location)
                .latitude(latitude)
                .longitude(longitude)
                .recordedAt(LocalDateTime.now(clock))
                .build();
        trackingEventRepository.save(trackingEvent);
        eventPublisher.publishEvent(new TrackingEventAddedEvent(shipment.getId()));
    }
}
