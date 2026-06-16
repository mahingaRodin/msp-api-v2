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
public class ConsentEventPayload {
    private UUID consentRequestId;
    private UUID tenantId;
    private String reason;
    private int durationHours;
    private String adminEmail;
    private String ownerEmail;
}
