package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @ManyToOne
    private Store store;

    /**
     * Links this category to its owning Business tenant.
     * Inherited from the parent store's tenantId at creation time.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

}
