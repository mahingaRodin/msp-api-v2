package com.msp.mappers;

import com.msp.models.CustomerStoreRelationship;
import com.msp.payloads.dtos.CustomerStoreRelationshipDto;

public class CustomerStoreRelationshipMapper {

    public static CustomerStoreRelationshipDto toDto(CustomerStoreRelationship rel) {
        if (rel == null) return null;

        CustomerStoreRelationshipDto dto = new CustomerStoreRelationshipDto();
        dto.setRelationshipId(rel.getId());
        dto.setFirstInteractionAt(rel.getFirstInteractionAt());
        dto.setLastInteractionAt(rel.getLastInteractionAt());
        dto.setNotes(rel.getNotes());

        if (rel.getCustomer() != null) {
            dto.setCustomerId(rel.getCustomer().getId());
            dto.setFirstName(rel.getCustomer().getFirstName());
            dto.setLastName(rel.getCustomer().getLastName());
            dto.setEmail(rel.getCustomer().getEmail());
            dto.setPhone(rel.getCustomer().getPhone());
        }

        if (rel.getStore() != null) {
            dto.setStoreId(rel.getStore().getId());
            dto.setStoreName(rel.getStore().getBrand());
        }

        return dto;
    }
}
