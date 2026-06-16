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
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /**
     * SKU is unique per store, not globally.
     * Two different stores can have the same SKU.
     */
    @Column(nullable = false)
    private String sku;

    /**
     * Links this product to its owning Business tenant.
     * Inherited from the parent store's tenantId at creation time.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    private String description;

    private Double mrp;

    private Double sellingPrice;
    private String brand;
    private String image;

    @ManyToOne
    private Category category;

    @ManyToOne
    private Store store;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
