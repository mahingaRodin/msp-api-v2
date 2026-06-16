package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.msp.enums.EAuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only audit trail. Rows are never updated or deleted.
 * A PostgreSQL trigger (V10 migration) enforces immutability at the DB level.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_tenant",  columnList = "tenant_id"),
                @Index(name = "idx_audit_actor",   columnList = "actor_user_id"),
                @Index(name = "idx_audit_created", columnList = "created_at"),
                @Index(name = "idx_audit_action",  columnList = "action")
        })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    @JsonIgnoreProperties({"password", "store", "branch"})
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private EAuditAction action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /** JSON blob with before/after state or contextual details. */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
