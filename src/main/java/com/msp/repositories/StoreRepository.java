package com.msp.repositories;

import com.msp.models.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    Store findByStoreAdminId(UUID adminId);
    Page<Store> findByTenantId(UUID tenantId, Pageable pageable);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
