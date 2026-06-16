package com.msp.controllers;

import com.msp.payloads.dtos.CustomerDto;
import com.msp.payloads.dtos.CustomerStoreRelationshipDto;
import com.msp.payloads.dtos.CustomerUpdateDto;
import com.msp.payloads.request.CustomerRegistrationRequest;
import com.msp.payloads.response.ApiResponse2;
import com.msp.payloads.response.CustomerRegistrationResponse;
import com.msp.services.ClientRegistrationService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Tag(name = "Client Registration",
     description = "Global client self-registration and store-scoped client management")
public class ClientRegistrationController {

    private final ClientRegistrationService clientRegistrationService;

    // ── Public — no auth required ────────────────────────────────────────────

    @Operation(
            summary = "Register a client account",
            description = "Creates a single global account. The client can then browse products " +
                          "from all stores and shop anywhere. No store selection needed at registration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = CustomerRegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<CustomerRegistrationResponse> register(
            @Valid @RequestBody CustomerRegistrationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientRegistrationService.register(request));
    }

    // ── Customer's own profile endpoints ─────────────────────────────────────

    @Operation(
            summary = "Get my profile",
            description = "Returns the authenticated client's global profile."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<CustomerDto> getMyProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(clientRegistrationService.getCustomer(id));
    }

    @Operation(
            summary = "Get stores I've shopped at",
            description = "Returns all stores the client has placed at least one order with."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{id}/stores")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<Page<CustomerStoreRelationshipDto>> getMyStores(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(clientRegistrationService.getMyStores(id, page, size));
    }

    @Operation(summary = "Update my profile")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<CustomerDto> updateMyProfile(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerUpdateDto dto
    ) {
        return ResponseEntity.ok(clientRegistrationService.updateCustomer(id, dto));
    }

    // ── Store portal — store staff sees only their own clients ─────────────

    @Operation(
            summary = "List clients for a store (store portal)",
            description = "Returns all clients who have placed at least one order at this store. " +
                          "A store can only see clients who have interacted with them."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer list returned"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/portal/stores/{storeId}")
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<CustomerStoreRelationshipDto>> getStoreClients(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                clientRegistrationService.getCustomersByStore(storeId, page, size));
    }

    @Operation(
            summary = "Search clients in a store (store portal)",
            description = "Searches by name or email within the store's client base."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/portal/stores/{storeId}/search")
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_BRANCH_MANAGER','ROLE_BRANCH_CASHIER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Page<CustomerStoreRelationshipDto>> searchStoreClients(
            @PathVariable UUID storeId,
            @Parameter(description = "Search keyword (name or email)") @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                clientRegistrationService.searchCustomersByStore(storeId, q, page, size));
    }

    @Operation(
            summary = "Get a specific client in store context (store portal)",
            description = "Returns the client's profile enriched with store-specific data " +
                          "(first interaction date, notes, etc.)."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/stores/{storeId}/{clientId}")
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_BRANCH_MANAGER','ROLE_BRANCH_CASHIER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerStoreRelationshipDto> getCustomerInStore(
            @PathVariable UUID storeId,
            @PathVariable UUID clientId
    ) {
        return ResponseEntity.ok(
                clientRegistrationService.getCustomerInStore(clientId, storeId));
    }

    @Operation(
            summary = "Update store notes for a client (store portal)",
            description = "Lets store staff add or update private notes about a client " +
                          "(e.g. VIP status, preferences). Does not modify the client's global profile."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/portal/stores/{storeId}/{clientId}/notes")
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<CustomerStoreRelationshipDto> updateStoreNotes(
            @PathVariable UUID storeId,
            @PathVariable UUID clientId,
            @RequestParam String notes
    ) {
        return ResponseEntity.ok(
                clientRegistrationService.updateStoreNotes(clientId, storeId, notes));
    }

    // ── Super admin ──────────────────────────────────────────────────────────

    @Operation(
            summary = "Delete a client account (super admin)",
            description = "Permanently removes the client's global account and all store relationships."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse2> deleteCustomer(@PathVariable UUID id) {
        clientRegistrationService.deleteCustomer(id);
        ApiResponse2 response = new ApiResponse2();
        response.setMessage("Customer account deleted successfully.");
        return ResponseEntity.ok(response);
    }
}
