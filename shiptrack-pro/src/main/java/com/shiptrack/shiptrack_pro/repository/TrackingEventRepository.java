package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findAllByShipmentIdOrderByRecordedAtAsc(Long shipmentId);
}
