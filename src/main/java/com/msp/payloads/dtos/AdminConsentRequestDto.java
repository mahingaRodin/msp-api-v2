package com.msp.payloads.dtos;

import com.msp.enums.EConsentStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AdminConsentRequestDto {
    private UUID id;
    private UUID tenantId;
    private UUID requestingAdminId;
    private String requestingAdminEmail;
    private String reason;
    private int requestedDurationHours;
    private EConsentStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private UUID revokedById;
}
