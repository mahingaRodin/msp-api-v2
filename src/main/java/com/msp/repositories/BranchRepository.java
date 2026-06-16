package com.msp.repositories;

import com.msp.models.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    Page<Branch> findByStoreId(UUID storeId, Pageable pageable);
    Page<Branch> findByTenantId(UUID tenantId, Pageable pageable);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
