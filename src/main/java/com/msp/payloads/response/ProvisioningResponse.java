package com.msp.payloads.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProvisioningResponse {
    private UUID tenantId;
    private UUID businessId;
    private String ownerEmail;
    private String message;
}
