package com.msp.repositories;

import com.msp.models.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /** Global email uniqueness check — used during registration. */
    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    /** Global search — used by super admin only. */
    Page<Customer> findByFirstNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String email, Pageable pageable);
}
