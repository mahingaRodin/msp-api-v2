package com.msp.controllers;

import com.msp.enums.ESubscriptionTier;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.payloads.dtos.BusinessDto;
import com.msp.services.TenantProvisioningService;
import com.msp.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Tenant owner's self-service portal.
 * Allows the business owner to view their own business profile and subscription status.
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tenant Portal", description = "Business owner self-service — profile and subscription")
public class TenantPortalController {

    private final UserService userService;
    private final TenantProvisioningService provisioningService;

    @Operation(summary = "Get my business profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Business profile returned"),
            @ApiResponse(responseCode = "403", description = "Not a business owner"),
            @ApiResponse(responseCode = "404", description = "Business not found for this tenant")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<BusinessDto> getMyProfile() {
        UUID tenantId = requireTenantId();
        return ResponseEntity.ok(provisioningService.getTenantDetails(tenantId));
    }

    @Operation(summary = "Get my subscription status",
               description = "Returns the current subscription tier, trial end date, and business status.")
    @GetMapping("/me/subscription")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<BusinessDto> getMySubscription() {
        UUID tenantId = requireTenantId();
        return ResponseEntity.ok(provisioningService.getTenantDetails(tenantId));
    }

    // ── Super admin management endpoints ─────────────────────────────────────

    @Operation(summary = "Deprovision a tenant (super admin)",
               description = "Sets the business status to DEPROVISIONED. Irreversible — use with caution.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant deprovisioned"),
            @ApiResponse(responseCode = "400", description = "Tenant already deprovisioned"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @PostMapping("/admin/{tenantId}/deprovision")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deprovisionTenant(@PathVariable UUID tenantId) {
        provisioningService.deprovisionTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update a tenant's subscription plan (super admin)",
               description = "Changes the subscription tier. Extension point for future billing integration.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan updated"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    @PatchMapping("/admin/{tenantId}/plan")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<BusinessDto> updateTenantPlan(
            @PathVariable UUID tenantId,
            @RequestParam ESubscriptionTier tier) {
        return ResponseEntity.ok(provisioningService.updateTenantPlan(tenantId, tier));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private UUID requireTenantId() {
        UUID tenantId = userService.getCurrentUser().getTenantId();
        if (tenantId == null) {
            throw new BusinessRegistrationException(
                    "Your account is not linked to a business tenant.");
        }
        return tenantId;
    }
}
