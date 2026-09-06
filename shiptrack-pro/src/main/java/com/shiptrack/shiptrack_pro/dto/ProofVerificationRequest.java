package com.shiptrack.shiptrack_pro.dto;

import com.shiptrack.shiptrack_pro.entity.ProofVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProofVerificationRequest {

    @NotNull(message = "Verification status is required")
    private ProofVerificationStatus status;

    @Size(max = 500, message = "Verification notes must not exceed 500 characters")
    private String notes;
}
