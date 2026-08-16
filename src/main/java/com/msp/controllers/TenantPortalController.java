package com.msp.controllers;

import com.msp.enums.ESubscriptionTier;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.exceptions.UserException;
import com.msp.payloads.dtos.BusinessDto;
import com.msp.payloads.request.SubscribeRequest;
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
        return ResponseEntity.ok(provisioningService.getTenantDetails(requireTenantId()));
    }

    @Operation(summary = "Get my subscription status")
    @GetMapping("/me/subscription")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<BusinessDto> getMySubscription() {
        return ResponseEntity.ok(provisioningService.getTenantDetails(requireTenantId()));
    }

    @Operation(summary = "Upgrade / switch plan with demo card payment")
    @PostMapping("/me/subscribe")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<BusinessDto> subscribe(@RequestBody SubscribeRequest request) {
        if (request.getTier() == null || request.getTier() == ESubscriptionTier.FREE_TRIAL) {
            throw new UserException("Choose a paid plan (BASIC or PREMIUM)");
        }
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()
                || request.getCardBrand() == null || request.getCardBrand().isBlank()) {
            throw new UserException("Enter demo card details to continue");
        }
        return ResponseEntity.ok(provisioningService.updateTenantPlan(requireTenantId(), request.getTier()));
    }

    @PostMapping("/admin/{tenantId}/deprovision")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deprovisionTenant(@PathVariable UUID tenantId) {
        provisioningService.deprovisionTenant(tenantId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/{tenantId}/plan")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<BusinessDto> updateTenantPlan(
            @PathVariable UUID tenantId,
            @RequestParam ESubscriptionTier tier) {
        return ResponseEntity.ok(provisioningService.updateTenantPlan(tenantId, tier));
    }

    private UUID requireTenantId() {
        UUID tenantId = userService.getCurrentUser().getTenantId();
        if (tenantId == null) {
            throw new BusinessRegistrationException(
                    "Your account is not linked to a business tenant.");
        }
        return tenantId;
    }
}
