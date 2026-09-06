package com.shiptrack.shiptrack_pro.repository;

import com.shiptrack.shiptrack_pro.entity.ProofOfDelivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProofOfDeliveryRepository extends JpaRepository<ProofOfDelivery, Long> {

    boolean existsByShipmentId(Long shipmentId);

    @EntityGraph(attributePaths = {"shipment", "shipment.createdBy", "shipment.assignedOperator", "submittedBy", "verifiedBy"})
    Optional<ProofOfDelivery> findByShipmentId(Long shipmentId);

    @EntityGraph(attributePaths = {"shipment", "shipment.createdBy", "shipment.assignedOperator"})
    Optional<ProofOfDelivery> findBySignatureUrlOrPhotoUrl(String signatureUrl, String photoUrl);
}
