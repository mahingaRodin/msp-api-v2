package com.msp.controllers;

import com.msp.payloads.dtos.AdminConsentRequestDto;
import com.msp.services.AdminConsentService;
import com.msp.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin Consent", description = "Privacy-safe admin access to tenant data via explicit owner consent")
public class AdminConsentController {

    private final AdminConsentService consentService;
    private final UserService userService;

    // ── Super admin endpoints ─────────────────────────────────────────────────

    @Operation(summary = "Request access to a tenant's data (super admin)",
               description = "Creates a PENDING consent request. The tenant owner must grant it before any data access is allowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Consent request created"),
            @ApiResponse(responseCode = "400", description = "Invalid durationHours or tenant not found")
    })
    @PostMapping("/api/admin/consent-requests")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<AdminConsentRequestDto> requestConsent(
            @RequestParam UUID tenantId,
            @RequestParam String reason,
            @RequestParam(defaultValue = "24") int durationHours) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consentService.requestConsent(tenantId, reason, durationHours));
    }

    @Operation(summary = "List my consent requests (super admin)")
    @GetMapping("/api/admin/consent-requests")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<AdminConsentRequestDto>> listMyRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID adminId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(consentService.listMyConsentRequests(
                adminId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"))));
    }

    @Operation(summary = "Check if active consent exists for a tenant (super admin)")
    @GetMapping("/api/admin/consent-requests/active")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Boolean> hasActiveConsent(@RequestParam UUID tenantId) {
        UUID adminId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(consentService.hasActiveConsent(tenantId, adminId));
    }

    @Operation(summary = "Revoke an active consent (super admin or tenant owner)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent revoked"),
            @ApiResponse(responseCode = "400", description = "Consent is not ACTIVE"),
            @ApiResponse(responseCode = "404", description = "Consent request not found")
    })
    @PostMapping("/api/admin/consent-requests/{id}/revoke")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN','ROLE_STORE_ADMIN')")
    public ResponseEntity<AdminConsentRequestDto> revokeConsent(@PathVariable UUID id) {
        UUID callerId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(consentService.revokeConsent(id, callerId));
    }

    // ── Tenant owner endpoints ────────────────────────────────────────────────

    @Operation(summary = "List consent requests for my tenant (tenant owner)",
               description = "Shows all admin access requests targeting the authenticated owner's tenant.")
    @GetMapping("/api/tenant/consent-requests")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<Page<AdminConsentRequestDto>> listTenantRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID tenantId = userService.getCurrentUser().getTenantId();
        return ResponseEntity.ok(consentService.listConsentRequests(
                tenantId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"))));
    }

    @Operation(summary = "Grant a consent request (tenant owner)",
               description = "Allows the admin to access your tenant's data for the requested duration.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent granted"),
            @ApiResponse(responseCode = "400", description = "Request is not PENDING"),
            @ApiResponse(responseCode = "404", description = "Consent request not found")
    })
    @PostMapping("/api/tenant/consent-requests/{id}/grant")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<AdminConsentRequestDto> grantConsent(@PathVariable UUID id) {
        UUID ownerId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(consentService.grantConsent(id, ownerId));
    }
}
