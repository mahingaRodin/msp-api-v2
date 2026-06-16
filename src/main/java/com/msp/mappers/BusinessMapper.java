package com.msp.mappers;

import com.msp.models.Business;
import com.msp.payloads.dtos.BusinessDto;

public class BusinessMapper {

    public static BusinessDto toDto(Business business) {
        if (business == null) return null;

        BusinessDto dto = new BusinessDto();
        dto.setId(business.getId());
        dto.setTenantId(business.getTenantId());
        dto.setBusinessName(business.getBusinessName());
        dto.setLegalName(business.getLegalName());
        dto.setRegistrationNumber(business.getRegistrationNumber());
        dto.setCountry(business.getCountry());
        dto.setIndustry(business.getIndustry());
        dto.setDescription(business.getDescription());
        dto.setSubscriptionTier(business.getSubscriptionTier());
        dto.setStatus(business.getStatus());
        dto.setTrialEndsAt(business.getTrialEndsAt());
        dto.setCreatedAt(business.getCreatedAt());
        dto.setUpdatedAt(business.getUpdatedAt());

        if (business.getOwner() != null) {
            dto.setOwnerUserId(business.getOwner().getId());
            dto.setOwnerEmail(business.getOwner().getEmail());
            dto.setOwnerFullName(
                    business.getOwner().getFirstName() + " " + business.getOwner().getLastName()
            );
        }

        return dto;
    }
}
