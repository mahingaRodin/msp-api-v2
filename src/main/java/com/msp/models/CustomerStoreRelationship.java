package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Created automatically the first time a customer places an order at a store.
 * Gives that store visibility into the customer — order history, refunds, notes.
 * A customer can have relationships with many stores; a store can have many customers.
 * Neither side can see the other's data without this relationship existing.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "customer_store_relationships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name  = "uq_customer_store",
                        columnNames = {"customer_id", "store_id"})
        },
        indexes = {
                @Index(name = "idx_csr_customer", columnList = "customer_id"),
                @Index(name = "idx_csr_store",    columnList = "store_id")
        })
public class CustomerStoreRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnoreProperties({"storeAdmin", "contact", "hibernateLazyInitializer", "handler"})
    private Store store;

    /**
     * Timestamp of the first interaction (first order placed at this store).
     * Set once and never updated.
     */
    @Column(name = "first_interaction_at", nullable = false, updatable = false)
    private LocalDateTime firstInteractionAt;

    /**
     * Last time the customer interacted with this store (order, refund, etc.).
     * Updated on every interaction.
     */
    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    /** Optional store-specific notes about this customer (e.g. VIP, allergies). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (firstInteractionAt == null) {
            firstInteractionAt = LocalDateTime.now();
        }
        lastInteractionAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastInteractionAt = LocalDateTime.now();
    }
}
