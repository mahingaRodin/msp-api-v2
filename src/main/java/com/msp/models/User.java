package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import jakarta.persistence.*;

import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "users")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    @Email(message = "Email Should be valid!")
    private String email;
    private String phone;

    @ManyToOne(optional = true)
    @JsonIgnoreProperties({ "storeAdmin" })
    private Store store;

    @ManyToOne(optional = true)
    @JsonIgnoreProperties({ "manager", "store" })
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EUserRole role;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'ACTIVE'")
    private EUserStatus userStatus;
    
    /**
     * Links this user to their Business tenant.
     * Null for ROLE_SUPER_ADMIN. Set for all tenant-scoped users.
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    private LocalDateTime suspendedAt;
    private LocalDateTime dischargedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

}
