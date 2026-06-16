package com.msp.mappers;

import com.msp.models.TenantRegistration;
import com.msp.payloads.dtos.TenantRegistrationDto;

public class TenantRegistrationMapper {

    public static TenantRegistrationDto toDto(TenantRegistration reg) {
        if (reg == null) return null;

        TenantRegistrationDto dto = new TenantRegistrationDto();
        dto.setId(reg.getId());
        dto.setOwnerFirstName(reg.getOwnerFirstName());
        dto.setOwnerLastName(reg.getOwnerLastName());
        dto.setOwnerEmail(reg.getOwnerEmail());
        dto.setOwnerPhone(reg.getOwnerPhone());
        dto.setBusinessName(reg.getBusinessName());
        dto.setLegalName(reg.getLegalName());
        dto.setRegistrationNumber(reg.getRegistrationNumber());
        dto.setCountry(reg.getCountry());
        dto.setIndustry(reg.getIndustry());
        dto.setBusinessDescription(reg.getBusinessDescription());
        dto.setStatus(reg.getStatus());
        dto.setAdminNotes(reg.getAdminNotes());
        dto.setRejectionReason(reg.getRejectionReason());
        dto.setSubmittedAt(reg.getSubmittedAt());
        dto.setReviewedAt(reg.getReviewedAt());
        dto.setProvisionedTenantId(reg.getProvisionedTenantId());
        dto.setDocumentS3Keys(reg.getDocumentS3Keys() != null ? new java.util.ArrayList<>(reg.getDocumentS3Keys()) : new java.util.ArrayList<>());

        if (reg.getReviewedBy() != null) {
            dto.setReviewedById(reg.getReviewedBy().getId());
            dto.setReviewedByName(
                    reg.getReviewedBy().getFirstName() + " " + reg.getReviewedBy().getLastName()
            );
        }

        return dto;
    }
}
