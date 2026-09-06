package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Notification;
import com.shiptrack.shiptrack_pro.entity.NotificationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"user", "shipment"})
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "shipment"})
    Optional<Notification> findOneByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "shipment"})
    Optional<Notification> findFirstByUserIdAndShipmentIdAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
            Long userId,
            Long shipmentId,
            NotificationType type,
            LocalDateTime createdAfter
    );
}
