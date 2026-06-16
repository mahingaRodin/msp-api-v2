package com.msp.payloads.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationDecisionRequest {

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String adminNotes;

    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    private String rejectionReason;
}
