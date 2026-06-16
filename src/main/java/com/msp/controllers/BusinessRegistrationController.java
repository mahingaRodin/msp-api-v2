package com.msp.controllers;

import com.msp.enums.ERegistrationStatus;
import com.msp.payloads.dtos.TenantRegistrationDto;
import com.msp.payloads.request.BusinessRegistrationRequest;
import com.msp.payloads.request.RegistrationDecisionRequest;
import com.msp.payloads.response.ProvisioningResponse;
import com.msp.payloads.response.RegistrationSubmittedResponse;
import com.msp.services.BusinessRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.msp.services.AwsS3Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Business Registration", description = "Business owner registration and admin approval APIs")
public class BusinessRegistrationController {

    private final BusinessRegistrationService registrationService;
    private final AwsS3Service s3Service;

    // ── Public endpoints ────────────────────────────────────────────────────

    @Operation(
            summary = "Submit a new business registration",
            description = "Any applicant can submit a registration request. No authentication required. " +
                          "The system admin will review and approve or reject it."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Registration submitted and pending review",
                    content = @Content(schema = @Schema(implementation = RegistrationSubmittedResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email or business name already registered"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/api/registrations")
    public ResponseEntity<RegistrationSubmittedResponse> submitRegistration(
            @Valid @RequestBody BusinessRegistrationRequest request
    ) {
        TenantRegistrationDto dto = registrationService.submitRegistration(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new RegistrationSubmittedResponse(
                        dto.getId(),
                        dto.getStatus(),
                        "Registration submitted successfully. You will be notified once reviewed."
                )
        );
    }

    @Operation(summary = "Check registration status by ID",
               description = "Allows an applicant to poll the status of their registration.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    @GetMapping("/api/registrations/{id}/status")
    public ResponseEntity<TenantRegistrationDto> getRegistrationStatus(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    @Operation(summary = "Upload supporting documents for a registration",
               description = "Allows an applicant to upload supporting documents to S3.")
    @PostMapping(value = "/api/registrations/{id}/documents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String s3Key = s3Service.uploadDocument(
                    "pending-" + id.toString(),
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getContentType()
            );

            registrationService.addDocumentKey(id, s3Key);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Admin endpoints ─────────────────────────────────────────────────────

    @Operation(
            summary = "List all registrations (admin)",
            description = "Returns a paginated list of business registrations. Optionally filter by status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned"),
            @ApiResponse(responseCode = "403", description = "Requires SUPER_ADMIN role")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/api/admin/registrations")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<TenantRegistrationDto>> listRegistrations(
            @Parameter(description = "Filter by status: PENDING, UNDER_REVIEW, APPROVED, REJECTED")
            @RequestParam(required = false) ERegistrationStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(registrationService.listRegistrations(status, pageable));
    }

    @Operation(summary = "Get a single registration detail (admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "403", description = "Requires SUPER_ADMIN role")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/api/admin/registrations/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<TenantRegistrationDto> getRegistration(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    @Operation(
            summary = "Mark registration as Under Review (admin)",
            description = "Signals that the admin has started reviewing this registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated to UNDER_REVIEW"),
            @ApiResponse(responseCode = "422", description = "Invalid state transition"),
            @ApiResponse(responseCode = "403", description = "Requires SUPER_ADMIN role")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/api/admin/registrations/{id}/review")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<TenantRegistrationDto> markUnderReview(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(registrationService.markUnderReview(id));
    }

    @Operation(
            summary = "Approve a registration and provision the tenant (admin)",
            description = "Approves the registration, creates the Business entity and the owner's account. " +
                          "The owner receives login credentials."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant provisioned",
                    content = @Content(schema = @Schema(implementation = ProvisioningResponse.class))),
            @ApiResponse(responseCode = "422", description = "Invalid state transition or already provisioned"),
            @ApiResponse(responseCode = "403", description = "Requires SUPER_ADMIN role")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/api/admin/registrations/{id}/approve")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ProvisioningResponse> approveRegistration(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RegistrationDecisionRequest body
    ) {
        String notes = body != null ? body.getAdminNotes() : null;
        return ResponseEntity.ok(registrationService.approveRegistration(id, notes));
    }

    @Operation(
            summary = "Resubmit a rejected registration (applicant)",
            description = "Allows the original applicant to update and resubmit a REJECTED registration. Resets status to PENDING."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration resubmitted"),
            @ApiResponse(responseCode = "400", description = "Registration is not in REJECTED status"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    @PostMapping("/api/registrations/{id}/resubmit")
    public ResponseEntity<TenantRegistrationDto> resubmitRegistration(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessRegistrationRequest body
    ) {
        return ResponseEntity.ok(registrationService.resubmitRegistration(id, body));
    }

    @Operation(
            summary = "Reject a registration (admin)",
            description = "Rejects the registration with a reason. The applicant will be notified."
    )    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration rejected"),
            @ApiResponse(responseCode = "422", description = "Invalid state transition"),
            @ApiResponse(responseCode = "403", description = "Requires SUPER_ADMIN role")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/api/admin/registrations/{id}/reject")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<TenantRegistrationDto> rejectRegistration(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationDecisionRequest body
    ) {
        return ResponseEntity.ok(
                registrationService.rejectRegistration(id, body.getRejectionReason())
        );
    }
}
