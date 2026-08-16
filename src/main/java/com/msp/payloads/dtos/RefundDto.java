package com.msp.payloads.dtos;

import com.msp.enums.EPaymentType;
import com.msp.models.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RefundDto {
    private UUID id;
    private Order order;
    private UUID orderId;
    private String reason;
    private Double amount;
    private UUID shiftReportId;
    private UserDto cashier;
    private String cashierName;
    private BranchDto branch;
    private UUID branchId;
    private EPaymentType paymentType;
    private LocalDateTime createdAt;
    private String status;
    private boolean restocked;
}
