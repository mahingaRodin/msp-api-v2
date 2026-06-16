package com.msp.controllers;

import com.msp.models.AuditLog;
import com.msp.services.AdminConsentService;
import com.msp.services.AuditLogService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Audit Logs", description = "Immutable audit trail for tenant and system events")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AdminConsentService consentService;
    private final UserService userService;

    @Operation(summary = "View my tenant's audit log (tenant owner)",
               description = "Returns the audit trail for the authenticated owner's tenant.")
    @GetMapping("/api/tenant/audit-logs")
    @PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
    public ResponseEntity<Page<AuditLog>> getMyTenantLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = userService.getCurrentUser().getTenantId();
        return ResponseEntity.ok(auditLogService.getLogsForTenant(
                tenantId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Operation(summary = "View a specific tenant's audit log (super admin — requires active consent)",
               description = "Admin can only access this when the tenant owner has granted an active consent window.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs returned"),
            @ApiResponse(responseCode = "403", description = "No active consent for this tenant")
    })
    @GetMapping("/api/admin/tenant/{tenantId}/audit-logs")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLog>> getTenantLogsAsAdmin(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID adminId = userService.getCurrentUser().getId();
        if (!consentService.hasActiveConsent(tenantId, adminId)) {
            return ResponseEntity.status(403).build();
        }

        // Write ADMIN_DATA_ACCESS audit entry every time admin reads tenant data
        auditLogService.log(tenantId, userService.getCurrentUser(),
                com.msp.enums.EAuditAction.ADMIN_DATA_ACCESS,
                "AuditLog", tenantId.toString(),
                "admin accessed tenant audit logs", null);

        return ResponseEntity.ok(auditLogService.getLogsForTenant(
                tenantId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Operation(summary = "View system-wide audit log (super admin)")
    @GetMapping("/api/admin/audit-logs")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.getAllLogs(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
