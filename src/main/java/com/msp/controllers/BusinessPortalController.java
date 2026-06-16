package com.msp.controllers;

import com.msp.payloads.dtos.BranchDto;
import com.msp.payloads.dtos.StoreDto;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.request.CreateBranchRequest;
import com.msp.payloads.request.CreateEmployeeRequest;
import com.msp.payloads.request.CreateStoreRequest;
import com.msp.payloads.response.ApiResponse2;
import com.msp.services.BusinessPortalService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/portal/business")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ROLE_STORE_ADMIN')")
@Tag(name = "Business Portal",
     description = "Store, branch, and employee management for approved business owners")
public class BusinessPortalController {

    private final BusinessPortalService portalService;

    // ── Store endpoints ──────────────────────────────────────────────────────

    @Operation(summary = "Create a store under my business")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Store created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Account not linked to a tenant")
    })
    @PostMapping("/stores")
    public ResponseEntity<StoreDto> createStore(
            @Valid @RequestBody CreateStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.createStore(request));
    }

    @Operation(summary = "List all my stores")
    @GetMapping("/stores")
    public ResponseEntity<Page<StoreDto>> getMyStores(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(portalService.getMyStores(page, size));
    }

    @Operation(summary = "Update one of my stores")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store updated"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @PutMapping("/stores/{storeId}")
    public ResponseEntity<StoreDto> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateStoreRequest request) {
        return ResponseEntity.ok(portalService.updateStore(storeId, request));
    }

    @Operation(summary = "Delete one of my stores")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store deleted"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @DeleteMapping("/stores/{storeId}")
    public ResponseEntity<ApiResponse2> deleteStore(@PathVariable UUID storeId) {
        portalService.deleteStore(storeId);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Store deleted successfully.");
        return ResponseEntity.ok(res);
    }

    // ── Branch endpoints ─────────────────────────────────────────────────────

    @Operation(summary = "Create a branch under one of my stores",
               description = "The storeId in the request body must belong to your tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Branch created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @PostMapping("/branches")
    public ResponseEntity<BranchDto> createBranch(
            @Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.createBranch(request));
    }

    @Operation(summary = "List branches for one of my stores")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch list returned"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @GetMapping("/stores/{storeId}/branches")
    public ResponseEntity<Page<BranchDto>> getStoreBranches(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(portalService.getStoreBranches(storeId, page, size));
    }

    @Operation(summary = "Update one of my branches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch updated"),
            @ApiResponse(responseCode = "403", description = "Branch belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @PutMapping("/branches/{branchId}")
    public ResponseEntity<BranchDto> updateBranch(
            @PathVariable UUID branchId,
            @Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.ok(portalService.updateBranch(branchId, request));
    }

    @Operation(summary = "Delete one of my branches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Branch deleted"),
            @ApiResponse(responseCode = "403", description = "Branch belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @DeleteMapping("/branches/{branchId}")
    public ResponseEntity<ApiResponse2> deleteBranch(@PathVariable UUID branchId) {
        portalService.deleteBranch(branchId);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Branch deleted successfully.");
        return ResponseEntity.ok(res);
    }

    // ── Employee endpoints ───────────────────────────────────────────────────

    @Operation(summary = "Create an employee for one of my stores",
               description = "Allowed roles: ROLE_STORE_MANAGER, ROLE_BRANCH_MANAGER, ROLE_BRANCH_CASHIER. " +
                             "branchId is required for branch-level roles.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid role"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Store or branch not found"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping("/stores/{storeId}/employees")
    public ResponseEntity<UserDto> createEmployee(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portalService.createEmployee(storeId, request));
    }

    @Operation(summary = "List employees for one of my stores")
    @GetMapping("/stores/{storeId}/employees")
    public ResponseEntity<Page<UserDto>> getStoreEmployees(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(portalService.getStoreEmployees(storeId, page, size));
    }

    @Operation(summary = "Update an employee's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated"),
            @ApiResponse(responseCode = "403", description = "Employee belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PutMapping("/employees/{employeeId}")
    public ResponseEntity<UserDto> updateEmployee(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(portalService.updateEmployee(employeeId, request));
    }

    @Operation(summary = "Discharge (deactivate) an employee",
               description = "Sets status to DISCHARGED. Does not delete — history is preserved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee discharged"),
            @ApiResponse(responseCode = "400", description = "Employee already discharged"),
            @ApiResponse(responseCode = "403", description = "Employee belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PatchMapping("/employees/{employeeId}/discharge")
    public ResponseEntity<UserDto> dischargeEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(portalService.dischargeEmployee(employeeId));
    }
}
