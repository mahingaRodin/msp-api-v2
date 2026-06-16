package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Table(name = "branches")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String address;
    private String phone;
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> workingDays = new java.util.ArrayList<>();

    private LocalTime openTime;
    private LocalTime closeTime;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JsonIgnoreProperties({ "storeAdmin", "branch", "contact" })
    private Store store;

    /**
     * Links this branch to its owning Business tenant.
     * Inherited from the parent store's tenantId at creation time.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;


    @OneToOne
    @JsonIgnoreProperties({ "branch", "store", "password" })
    private User manager;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
