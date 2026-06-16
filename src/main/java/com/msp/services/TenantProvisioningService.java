package com.msp.services;

import com.msp.enums.ESubscriptionTier;
import com.msp.payloads.dtos.BusinessDto;
import com.msp.payloads.response.ProvisioningResponse;

import java.util.UUID;

public interface TenantProvisioningService {

    /**
     * Creates the Business entity and the owner User (ROLE_STORE_ADMIN) from an approved registration.
     * Sets FREE_TRIAL tier and links tenantId to the owner's JWT claims.
     *
     * Precondition: registrationId maps to an APPROVED TenantRegistration with no provisionedTenantId yet.
     * Postcondition: Business.tenantId is a stable UUID; User.tenantId == Business.tenantId.
     * If provisioning fails mid-way the transaction rolls back entirely.
     */
    ProvisioningResponse provisionTenant(UUID registrationId);

    /**
     * Returns the Business profile for a given tenantId.
     */
    BusinessDto getTenantDetails(UUID tenantId);

    /**
     * Soft-deletes a tenant. Sets Business.status = DEPROVISIONED.
     * SUPER_ADMIN only. Precondition: no active orders in progress.
     */
    void deprovisionTenant(UUID tenantId);

    /**
     * Changes the subscription tier. Extension point for future billing integration.
     * Postcondition: Business.subscriptionTier updated; SUBSCRIPTION_CHANGED audit log written.
     */
    BusinessDto updateTenantPlan(UUID tenantId, ESubscriptionTier tier);
}
