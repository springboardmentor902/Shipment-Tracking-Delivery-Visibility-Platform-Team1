package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.ETAPrediction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ETAPredictionRepository extends JpaRepository<ETAPrediction, Long> {

    @EntityGraph(attributePaths = {"shipment", "shipment.createdBy", "shipment.assignedOperator"})
    Optional<ETAPrediction> findByShipmentId(Long shipmentId);
}
