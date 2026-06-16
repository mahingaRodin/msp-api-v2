package com.msp.services;

import com.msp.models.User;
import com.msp.payloads.dtos.BranchDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface BranchService {
    BranchDto createBranch(BranchDto branchDto);
    BranchDto updateBranch(UUID id, BranchDto branchDto) throws Exception;
    void deleteBranch(UUID id) throws Exception;
    Page<BranchDto> getAllBranchesByStoreId(UUID storeId, int page, int size);
    BranchDto getBranchById(UUID id) throws Exception;
    Page<BranchDto> getAllBranches(int page, int size);
}
