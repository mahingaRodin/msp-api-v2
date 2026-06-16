package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.msp.enums.EBusinessStatus;
import com.msp.enums.ESubscriptionTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "businesses",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "tenant_id"),
                @UniqueConstraint(columnNames = "business_name")
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Stable external identifier embedded in every JWT for tenant-scoped users.
     * Generated once at provisioning and never changes.
     */
    @Column(name = "tenant_id", nullable = false, unique = true, updatable = false)
    private UUID tenantId;

    @Column(name = "business_name", nullable = false, unique = true)
    private String businessName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(length = 2)
    private String country;

    @Column(length = 100)
    private String industry;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 50)
    private ESubscriptionTier subscriptionTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EBusinessStatus status;

    /**
     * The business owner — always ROLE_STORE_ADMIN.
     * Created automatically during tenant provisioning.
     */
    @OneToOne
    @JoinColumn(name = "owner_user_id", nullable = false)
    @JsonIgnoreProperties({"password", "store", "branch"})
    private User owner;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (subscriptionTier == null) {
            subscriptionTier = ESubscriptionTier.FREE_TRIAL;
        }
        if (status == null) {
            status = EBusinessStatus.ACTIVE;
        }
        if (subscriptionTier == ESubscriptionTier.FREE_TRIAL && trialEndsAt == null) {
            trialEndsAt = createdAt.plusDays(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
