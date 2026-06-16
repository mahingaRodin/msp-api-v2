package com.msp.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.msp.enums.EPaymentType;
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
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Order order;

    private String reason;

    private Double amount;

    @ManyToOne
    @JsonIgnore
    private ShiftReport shiftReport;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Branch branch;

    private EPaymentType paymentType;
    private LocalDateTime createdAt;

    /** Links this refund to its owning Business tenant. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
