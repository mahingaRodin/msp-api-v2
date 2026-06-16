package com.msp.models;

import com.msp.enums.EUserRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "customers",
        indexes = {
                @Index(name = "idx_customer_email", columnList = "email", unique = true)
        })
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /**
     * Globally unique — one account per person across the entire platform.
     */
    @Column(nullable = false, unique = true)
    @Email(message = "Email Should Be Valid!")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EUserRole role;

    private String phone;

    /** BCrypt-hashed password. */
    private String password;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
