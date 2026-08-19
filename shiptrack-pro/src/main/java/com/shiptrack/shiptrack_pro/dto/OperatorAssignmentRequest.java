package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OperatorAssignmentRequest {
    @NotNull(message = "Operator id is required")
    private Long operatorId;
}
