package com.msp.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationEventPayload {
    private String eventType; // e.g., "REGISTRATION_SUBMITTED", "REGISTRATION_REJECTED", "TENANT_PROVISIONED"
    private UUID registrationId;
    private String ownerEmail;
    private String ownerFirstName;
    private String businessName;
    private String content; // Dynamic body or extra variables
}
