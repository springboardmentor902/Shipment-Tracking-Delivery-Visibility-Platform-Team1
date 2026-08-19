package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.Route;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    boolean existsByShipmentId(Long shipmentId);

    @EntityGraph(attributePaths = {
            "shipment", "shipment.createdBy", "shipment.assignedOperator", "createdBy"
    })
    Optional<Route> findByShipmentId(Long shipmentId);

    @EntityGraph(attributePaths = {
            "shipment", "shipment.createdBy", "shipment.assignedOperator", "createdBy"
    })
    Optional<Route> findOneById(Long id);
}
