package com.msp.mappers;

import com.msp.models.Order;
import com.msp.payloads.dtos.OrderDto;

public class OrderMapper {
    public static OrderDto toDto(Order order) {
        if (order == null) return null;

        return OrderDto.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .branchId(order.getBranch()   != null ? order.getBranch().getId()   : null)
                .cashierId(order.getCashier() != null ? order.getCashier().getId()  : null)
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .paymentType(order.getPaymentType())
                .status(order.getStatus())
                .items(order.getItems() == null ? java.util.List.of() :
                        order.getItems().stream().map(OrderItemMapper::toDto).toList())
                .build();
    }
}
