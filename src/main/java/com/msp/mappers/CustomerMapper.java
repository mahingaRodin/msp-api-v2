package com.msp.mappers;

import com.msp.models.Customer;
import com.msp.payloads.dtos.CustomerDto;

public class CustomerMapper {

    public static CustomerDto toDto(Customer customer) {
        if (customer == null) return null;

        return CustomerDto.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .role(customer.getRole())
                .phone(customer.getPhone())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public static Customer toEntity(CustomerDto dto) {
        if (dto == null) return null;

        return Customer.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .role(dto.getRole())
                .phone(dto.getPhone())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
