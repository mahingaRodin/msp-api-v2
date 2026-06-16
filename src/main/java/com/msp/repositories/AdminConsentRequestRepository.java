package com.msp.repositories;

import com.msp.enums.EConsentStatus;
import com.msp.models.AdminConsentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AdminConsentRequestRepository extends JpaRepository<AdminConsentRequest, UUID> {

    Page<AdminConsentRequest> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AdminConsentRequest> findByRequestingAdminId(UUID adminId, Pageable pageable);

    /**
     * Finds an active, non-expired consent window for a specific admin + tenant pair.
     * Used by the consent gate to allow or deny admin data access.
     */
    @Query("""
            SELECT r FROM AdminConsentRequest r
            WHERE r.tenantId = :tenantId
              AND r.requestingAdmin.id = :adminId
              AND r.status = 'ACTIVE'
              AND r.expiresAt > :now
            """)
    Optional<AdminConsentRequest> findActiveConsent(
            @Param("tenantId") UUID tenantId,
            @Param("adminId")  UUID adminId,
            @Param("now")      LocalDateTime now);
}
