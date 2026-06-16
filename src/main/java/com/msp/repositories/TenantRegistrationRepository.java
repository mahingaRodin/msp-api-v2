package com.msp.repositories;

import com.msp.enums.ERegistrationStatus;
import com.msp.models.TenantRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRegistrationRepository extends JpaRepository<TenantRegistration, UUID> {

    boolean existsByOwnerEmail(String ownerEmail);

    boolean existsByBusinessName(String businessName);

    Optional<TenantRegistration> findByOwnerEmail(String ownerEmail);

    Page<TenantRegistration> findByStatus(ERegistrationStatus status, Pageable pageable);
}
