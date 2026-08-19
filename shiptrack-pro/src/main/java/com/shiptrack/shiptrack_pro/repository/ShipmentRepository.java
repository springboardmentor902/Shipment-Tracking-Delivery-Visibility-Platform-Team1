package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Shipment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @EntityGraph(attributePaths = {"packages", "createdBy", "assignedOperator"})
    List<Shipment> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"packages", "createdBy", "assignedOperator"})
    List<Shipment> findAllByCreatedByIdOrderByCreatedAtDesc(Long createdById);

    @EntityGraph(attributePaths = {"packages", "createdBy", "assignedOperator"})
    List<Shipment> findAllByAssignedOperatorIdOrderByCreatedAtDesc(Long operatorId);

    @EntityGraph(attributePaths = {"packages", "createdBy", "assignedOperator"})
    java.util.Optional<Shipment> findOneById(Long id);
}
