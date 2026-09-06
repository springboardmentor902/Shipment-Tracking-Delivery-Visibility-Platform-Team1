package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.ProofVerificationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ProofOfDeliveryResponse {
    Long id;
    Long shipmentId;
    String trackingNumber;
    String recipientName;
    String deliveryNotes;
    String signatureUrl;
    String photoUrl;
    Long submittedById;
    String submittedBy;
    LocalDateTime submittedAt;
    ProofVerificationStatus verificationStatus;
    Long verifiedById;
    String verifiedBy;
    LocalDateTime verifiedAt;
    String verificationNotes;
}
