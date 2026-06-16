package com.msp.models;

import com.msp.enums.EStoreStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "stores")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String brand;

    @ManyToOne
    @JoinColumn(name = "store_admin_id", nullable = false)
    @JsonIgnoreProperties({"store", "branch", "password"})
    private User storeAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String description;
    private String storeType;

    private EStoreStatus status;

    /**
     * Links this store to its owning Business tenant.
     * Set at store creation time from the owner's JWT tenantId.
     * Null for stores created before the multi-tenant migration.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Embedded
    private StoreContact contact = new StoreContact();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = EStoreStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
