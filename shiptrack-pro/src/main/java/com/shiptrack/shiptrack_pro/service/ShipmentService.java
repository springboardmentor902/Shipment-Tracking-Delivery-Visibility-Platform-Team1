package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.PackageRequest;
import com.shiptrack.shiptrack_pro.dto.ShipmentRequest;
import com.shiptrack.shiptrack_pro.entity.Package;
import com.shiptrack.shiptrack_pro.entity.Shipment;
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.PackageRepository;
import com.shiptrack.shiptrack_pro.repository.ShipmentRepository;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;

    // CREATE SHIPMENT + PACKAGES
    public Shipment createShipment(ShipmentRequest request, String userEmail) {

        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Shipment shipment = new Shipment();

        shipment.setTrackingNumber(
                "ST-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        shipment.setSenderName(request.getSenderName());
        shipment.setReceiverName(request.getReceiverName());
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setCustomer(customer);
        shipment.setStatus("CREATED");

        Shipment savedShipment = shipmentRepository.save(shipment);

        // SAVE MULTIPLE PACKAGES
        if (request.getPackages() != null) {

            for (PackageRequest packageRequest : request.getPackages()) {

                Package pkg = new Package();

                pkg.setDescription(packageRequest.getDescription());
                pkg.setWeight(packageRequest.getWeight());
                pkg.setQuantity(packageRequest.getQuantity());
                pkg.setShipment(savedShipment);

                packageRepository.save(pkg);
            }
        }

        // Load saved packages into shipment response
        savedShipment.setPackages(
                packageRepository.findByShipment(savedShipment)
        );

        return savedShipment;
    }

    // GET MY SHIPMENTS
    public List<Shipment> getMyShipments(String userEmail) {

        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Shipment> shipments =
                shipmentRepository.findByCustomer(customer);

        // Load packages for each shipment
        for (Shipment shipment : shipments) {
            shipment.setPackages(
                    packageRepository.findByShipment(shipment)
            );
        }

        return shipments;
    }

    // GET SHIPMENT DETAILS
    public Shipment getShipmentById(Long id, String userEmail) {

        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (!shipment.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException(
                    "You are not authorized to view this shipment"
            );
        }

        shipment.setPackages(
                packageRepository.findByShipment(shipment)
        );

        return shipment;
    }

    // UPDATE SHIPMENT
    public Shipment updateShipment(
            Long id,
            ShipmentRequest request,
            String userEmail) {

        Shipment shipment = getShipmentById(id, userEmail);

        if ("CANCELLED".equals(shipment.getStatus())) {
            throw new RuntimeException(
                    "Cancelled shipment cannot be updated"
            );
        }

        if (request.getSenderName() != null) {
            shipment.setSenderName(request.getSenderName());
        }

        if (request.getReceiverName() != null) {
            shipment.setReceiverName(request.getReceiverName());
        }

        if (request.getOrigin() != null) {
            shipment.setOrigin(request.getOrigin());
        }

        if (request.getDestination() != null) {
            shipment.setDestination(request.getDestination());
        }

        Shipment updatedShipment =
                shipmentRepository.save(shipment);

        updatedShipment.setPackages(
                packageRepository.findByShipment(updatedShipment)
        );

        return updatedShipment;
    }

    // CANCEL SHIPMENT
    public Shipment cancelShipment(Long id, String userEmail) {

        Shipment shipment = getShipmentById(id, userEmail);

        if ("DELIVERED".equals(shipment.getStatus())) {
            throw new RuntimeException(
                    "Delivered shipment cannot be cancelled"
            );
        }

        shipment.setStatus("CANCELLED");

        Shipment cancelledShipment =
                shipmentRepository.save(shipment);

        cancelledShipment.setPackages(
                packageRepository.findByShipment(cancelledShipment)
        );

        return cancelledShipment;
    }
}