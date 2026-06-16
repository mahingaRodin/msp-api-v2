package com.msp.repositories;

import com.msp.enums.EBusinessStatus;
import com.msp.models.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByTenantId(UUID tenantId);

    boolean existsByBusinessName(String businessName);

    boolean existsByTenantId(UUID tenantId);

    Page<Business> findByStatus(EBusinessStatus status, Pageable pageable);
}
