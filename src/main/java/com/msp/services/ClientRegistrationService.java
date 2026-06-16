package com.msp.services;

import com.msp.models.CustomerStoreRelationship;
import com.msp.payloads.dtos.CustomerDto;
import com.msp.payloads.dtos.CustomerStoreRelationshipDto;
import com.msp.payloads.dtos.CustomerUpdateDto;
import com.msp.payloads.request.CustomerRegistrationRequest;
import com.msp.payloads.response.CustomerRegistrationResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ClientRegistrationService {

    /**
     * Global self-registration — one account per person, platform-wide.
     * No store required. Email must be globally unique.
     */
    CustomerRegistrationResponse register(CustomerRegistrationRequest request);

    /**
     * Returns the customer's own global profile.
     */
    CustomerDto getCustomer(UUID customerId);

    /**
     * Returns all stores the customer has interacted with.
     * Used in the customer's own profile/dashboard.
     */
    Page<CustomerStoreRelationshipDto> getMyStores(UUID customerId, int page, int size);

    // ── Store portal operations (store sees only their own customers) ─────────

    /**
     * Returns all customers who have interacted with a given store, paginated.
     * Store staff only — a store can only see customers linked to them.
     */
    Page<CustomerStoreRelationshipDto> getCustomersByStore(UUID storeId, int page, int size);

    /**
     * Searches customers within a store by name or email keyword.
     */
    Page<CustomerStoreRelationshipDto> searchCustomersByStore(UUID storeId, String keyword, int page, int size);

    /**
     * Returns the store-specific relationship view of a single customer.
     * Used by store staff to view a customer's profile within their store context.
     */
    CustomerStoreRelationshipDto getCustomerInStore(UUID customerId, UUID storeId);

    /**
     * Updates store-specific notes about a customer (e.g. VIP, preferences).
     * Does NOT modify the customer's global profile.
     */
    CustomerStoreRelationshipDto updateStoreNotes(UUID customerId, UUID storeId, String notes);

    /**
     * Updates the customer's own global profile (name, phone).
     * Email changes require global uniqueness re-check.
     */
    CustomerDto updateCustomer(UUID customerId, CustomerUpdateDto dto);

    /**
     * Deletes the customer's global account.
     * Super admin only — removes the account and all store relationships.
     */
    void deleteCustomer(UUID customerId);

    /**
     * Creates or retrieves the relationship between a customer and a store.
     * Called automatically when a customer places their first order at a store.
     * Idempotent — safe to call multiple times.
     */
    CustomerStoreRelationship ensureRelationship(UUID customerId, UUID storeId);
}
