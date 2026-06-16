package com.msp.payloads.dtos;

import com.msp.enums.EOrderStatus;
import com.msp.enums.EPaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderDto {
        private UUID id;
        private Double totalAmount;
        private LocalDateTime createdAt;

        private UUID branchId;
        private UUID customerId;
        private UUID cashierId;

        private List<OrderItemDto> items;
        private EPaymentType paymentType;
        private EOrderStatus status;
}
