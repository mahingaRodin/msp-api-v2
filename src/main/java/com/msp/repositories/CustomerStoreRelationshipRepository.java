package com.msp.repositories;

import com.msp.models.Customer;
import com.msp.models.CustomerStoreRelationship;
import com.msp.models.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerStoreRelationshipRepository
        extends JpaRepository<CustomerStoreRelationship, UUID> {

    /** Check if a relationship already exists (idempotent creation). */
    boolean existsByCustomerAndStore(Customer customer, Store store);

    /** Find the relationship between a specific customer and store. */
    Optional<CustomerStoreRelationship> findByCustomerAndStore(Customer customer, Store store);

    /**
     * All customers who have interacted with a given store — paginated.
     * Used by store portal to list their customer base.
     */
    Page<CustomerStoreRelationship> findByStore(Store store, Pageable pageable);

    /**
     * All stores a customer has interacted with — paginated.
     * Used by the customer's own profile view.
     */
    Page<CustomerStoreRelationship> findByCustomer(Customer customer, Pageable pageable);

    /**
     * Store-scoped customer search by name or email keyword.
     */
    @Query("""
            SELECT r FROM CustomerStoreRelationship r
            WHERE r.store = :store
              AND (LOWER(r.customer.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.customer.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.customer.email)     LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<CustomerStoreRelationship> searchByStoreAndKeyword(
            @Param("store")    Store store,
            @Param("keyword")  String keyword,
            Pageable pageable);

    /** Count how many customers a store has. */
    long countByStore(Store store);
}
