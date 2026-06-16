package com.msp.payloads.response;

import com.msp.enums.ERegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RegistrationSubmittedResponse {
    private UUID registrationId;
    private ERegistrationStatus status;
    private String message;
}
