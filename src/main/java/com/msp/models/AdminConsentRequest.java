package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.msp.enums.EConsentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks a super-admin's request to temporarily access a tenant's data.
 * The tenant owner must explicitly grant access before any admin data access is allowed.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "admin_consent_requests",
        indexes = {
                @Index(name = "idx_consent_tenant",  columnList = "tenant_id"),
                @Index(name = "idx_consent_admin",   columnList = "requesting_admin_id"),
                @Index(name = "idx_consent_status",  columnList = "status"),
                @Index(name = "idx_consent_expires", columnList = "expires_at")
        })
public class AdminConsentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_admin_id", nullable = false)
    @JsonIgnoreProperties({"password", "store", "branch"})
    private User requestingAdmin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_duration_hours", nullable = false)
    private int requestedDurationHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EConsentStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_id")
    @JsonIgnoreProperties({"password", "store", "branch"})
    private User revokedBy;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) {
            status = EConsentStatus.PENDING;
        }
    }
}
