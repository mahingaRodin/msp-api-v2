package com.msp.models;

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
@Table(name = "shift_reports")
public class ShiftReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    private Double totalSales;
    private Double totalRefunds;
    private Double netSale;
    private int totalOrders;

    @ManyToOne
    private Branch branch;

    @ManyToOne
    private User cashier;

    /** Links this shift report to its owning Business tenant. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Transient
    private List<PaymentSummary> paymentSummaries;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Product> topSellingProducts;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Order> recentOrders;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Refund> refunds;

}
