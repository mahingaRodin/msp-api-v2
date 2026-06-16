package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.msp.enums.ERegistrationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "tenant_registrations",
        indexes = {
                @Index(name = "idx_reg_status", columnList = "status"),
                @Index(name = "idx_reg_email",  columnList = "owner_email")
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TenantRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    // ── Applicant info (not yet a User in the system) ──────────────────────
    @Column(name = "owner_first_name", nullable = false, length = 100)
    private String ownerFirstName;

    @Column(name = "owner_last_name", nullable = false, length = 100)
    private String ownerLastName;

    @Column(name = "owner_email", nullable = false, unique = true, length = 255)
    private String ownerEmail;

    @Column(name = "owner_phone", length = 30)
    private String ownerPhone;

    // ── Business info ───────────────────────────────────────────────────────
    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(length = 2)
    private String country;

    @Column(length = 100)
    private String industry;

    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;

    // ── Lifecycle ───────────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ERegistrationStatus status;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    @JsonIgnoreProperties({"password", "store", "branch"})
    private User reviewedBy;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * Set after approval — links back to the provisioned Business.tenantId.
     * Non-null means provisioning has already happened; prevents double-provisioning.
     */
    @Column(name = "provisioned_tenant_id")
    private UUID provisionedTenantId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "registration_documents", joinColumns = @JoinColumn(name = "registration_id"))
    @Column(name = "document_key", nullable = false)
    @Builder.Default
    private List<String> documentS3Keys = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (status == null) {
            status = ERegistrationStatus.PENDING;
        }
    }
}
