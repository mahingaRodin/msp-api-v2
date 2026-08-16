package com.msp.repositories;

import com.msp.models.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Inventory findByProductIdAndBranchId(UUID productId, UUID branchId);
    List<Inventory> findByProductId(UUID productId);
    Page<Inventory> findByBranchId(UUID branchId, Pageable pageable);
    long countByBranchId(UUID branchId);
}
