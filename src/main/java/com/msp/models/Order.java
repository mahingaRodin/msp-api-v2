package com.msp.models;

import com.msp.enums.EOrderStatus;
import com.msp.enums.EPaymentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double totalAmount;
    private LocalDateTime createdAt;

    @ManyToOne
    private Branch branch;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    private EPaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EOrderStatus status;

    /** Links this order to its owning Business tenant for isolation. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = EOrderStatus.PENDING;
        }
    }
}
