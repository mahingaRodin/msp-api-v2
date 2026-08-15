package com.msp.mappers;

import com.msp.models.Branch;
import com.msp.models.Store;
import com.msp.payloads.dtos.BranchDto;

public class BranchMapper {
    public static BranchDto toDto(Branch branch) {
        if (branch == null) {
            return null;
        }
        return BranchDto.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .workingDays(branch.getWorkingDays() != null ? new java.util.ArrayList<>(branch.getWorkingDays()) : new java.util.ArrayList<>())
                .openTime(branch.getOpenTime())
                .closeTime(branch.getCloseTime())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .storeId(branch.getStore() != null ? branch.getStore().getId() : null)
                .build();
    }

    public static Branch toEntity(BranchDto branchDto, Store store) {
        return Branch.builder()
                .name(branchDto.getName())
                .address(branchDto.getAddress())
                .store(store)
                .email(branchDto.getEmail())
                .phone(branchDto.getPhone())
                .openTime(branchDto.getOpenTime())
                .closeTime(branchDto.getCloseTime())
                .workingDays(branchDto.getWorkingDays())
                .tenantId(store != null ? store.getTenantId() : null)
                .createdAt(branchDto.getCreatedAt())
                .updatedAt(branchDto.getUpdatedAt())
                .build();
    }
}
