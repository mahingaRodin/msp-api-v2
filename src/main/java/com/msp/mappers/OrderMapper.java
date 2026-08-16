package com.msp.mappers;

import com.msp.models.Branch;
import com.msp.models.Customer;
import com.msp.models.Order;
import com.msp.models.Store;
import com.msp.payloads.dtos.OrderDto;
import org.hibernate.Hibernate;

public class OrderMapper {
    public static OrderDto toDto(Order order) {
        return toDto(order, true);
    }

    /** Admin list view — skip line items for speed. */
    public static OrderDto toListDto(Order order) {
        return toDto(order, false);
    }

    private static OrderDto toDto(Order order, boolean includeItems) {
        if (order == null) return null;

        OrderDto.OrderDtoBuilder builder = OrderDto.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .branchId(order.getBranch() != null ? order.getBranch().getId() : null)
                .cashierId(order.getCashier() != null ? order.getCashier().getId() : null)
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .paymentType(order.getPaymentType())
                .status(order.getStatus());

        Branch branch = order.getBranch();
        if (branch != null && Hibernate.isInitialized(branch)) {
            builder.branchName(branch.getName());
            Store store = branch.getStore();
            if (store != null && Hibernate.isInitialized(store)) {
                builder.storeBrand(store.getBrand());
            }
        }

        Customer customer = order.getCustomer();
        if (customer != null && Hibernate.isInitialized(customer)) {
            String first = customer.getFirstName() != null ? customer.getFirstName() : "";
            String last = customer.getLastName() != null ? customer.getLastName() : "";
            String name = (first + " " + last).trim();
            builder.customerName(name.isEmpty() ? customer.getEmail() : name);
        }

        if (includeItems) {
            builder.items(order.getItems() == null ? java.util.List.of() :
                    order.getItems().stream().map(OrderItemMapper::toDto).toList());
        } else {
            builder.items(java.util.List.of());
        }

        return builder.build();
    }
}
