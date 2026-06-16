package com.msp.services;

import com.msp.payloads.dtos.AdminConsentRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminConsentService {

    /**
     * Super admin requests access to a tenant's data.
     * Creates AdminConsentRequest(PENDING) and notifies the tenant owner.
     * Precondition: tenantId exists and is ACTIVE; durationHours in [1, 72].
     */
    AdminConsentRequestDto requestConsent(UUID tenantId, String reason, int durationHours);

    /**
     * Tenant owner grants the consent request.
     * Sets status=ACTIVE, expiresAt=now()+requestedDurationHours.
     * Writes CONSENT_GRANTED audit log entry.
     * Precondition: caller is the tenant owner; request is PENDING.
     */
    AdminConsentRequestDto grantConsent(UUID consentRequestId, UUID tenantOwnerId);

    /**
     * Revokes an active consent window.
     * Caller can be the tenant owner or a SUPER_ADMIN.
     * Writes CONSENT_REVOKED audit log entry.
     */
    AdminConsentRequestDto revokeConsent(UUID consentRequestId, UUID revokedById);

    /**
     * Checks whether a valid (non-expired, non-revoked) consent window exists
     * for the given admin + tenant pair.
     * Used by the consent gate before allowing admin data access.
     */
    boolean hasActiveConsent(UUID tenantId, UUID adminUserId);

    /** Lists all consent requests for a tenant. */
    Page<AdminConsentRequestDto> listConsentRequests(UUID tenantId, Pageable pageable);

    /** Lists all consent requests made by a specific admin. */
    Page<AdminConsentRequestDto> listMyConsentRequests(UUID adminId, Pageable pageable);
}
