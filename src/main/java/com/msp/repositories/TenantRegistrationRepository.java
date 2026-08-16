package com.msp.repositories;

import com.msp.enums.ERegistrationStatus;
import com.msp.models.TenantRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantRegistrationRepository extends JpaRepository<TenantRegistration, UUID> {

    boolean existsByOwnerEmail(String ownerEmail);

    boolean existsByBusinessName(String businessName);

    Optional<TenantRegistration> findByOwnerEmail(String ownerEmail);

    @EntityGraph(attributePaths = "reviewedBy")
    Page<TenantRegistration> findByStatus(ERegistrationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "reviewedBy")
    @Query("select r from TenantRegistration r")
    Page<TenantRegistration> findAllWithReviewer(Pageable pageable);

    @EntityGraph(attributePaths = "reviewedBy")
    @Query("select r from TenantRegistration r where r.id = :id")
    Optional<TenantRegistration> findByIdWithReviewer(@Param("id") UUID id);
}
