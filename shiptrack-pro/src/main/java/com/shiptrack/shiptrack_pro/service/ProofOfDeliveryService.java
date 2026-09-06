package com.shiptrack.shiptrack_pro.service;

import com.shiptrack.shiptrack_pro.dto.ProofOfDeliveryResponse;
import com.shiptrack.shiptrack_pro.dto.ProofVerificationRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ProofOfDeliveryService {
    ProofOfDeliveryResponse submit(
            Long shipmentId,
            String recipientName,
            String deliveryNotes,
            MultipartFile signature,
            MultipartFile photo,
            String requesterEmail
    );

    ProofOfDeliveryResponse verify(
            Long shipmentId,
            ProofVerificationRequest request,
            String requesterEmail
    );

    ProofOfDeliveryResponse getByShipmentId(Long shipmentId, String requesterEmail);

    Resource getStoredFile(String fileName, String requesterEmail);
}
