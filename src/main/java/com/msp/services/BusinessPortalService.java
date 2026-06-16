package com.msp.services;

import com.msp.payloads.dtos.BranchDto;
import com.msp.payloads.dtos.StoreDto;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.request.CreateBranchRequest;
import com.msp.payloads.request.CreateEmployeeRequest;
import com.msp.payloads.request.CreateStoreRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface BusinessPortalService {

    // ── Store management ─────────────────────────────────────────────────────

    /**
     * Creates a store under the calling owner's tenant.
     * tenantId is taken from the authenticated user — not from the request body.
     */
    StoreDto createStore(CreateStoreRequest request);

    /** Returns all stores belonging to the calling owner's tenant. */
    Page<StoreDto> getMyStores(int page, int size);

    /** Updates a store. Only the tenant owner can update their own stores. */
    StoreDto updateStore(UUID storeId, CreateStoreRequest request);

    /** Deletes a store owned by the calling tenant. */
    void deleteStore(UUID storeId);

    // ── Branch management ────────────────────────────────────────────────────

    /**
     * Creates a branch under a store that belongs to the calling owner's tenant.
     * tenantId is inherited from the parent store.
     */
    BranchDto createBranch(CreateBranchRequest request);

    /** Returns all branches for a store owned by the calling tenant. */
    Page<BranchDto> getStoreBranches(UUID storeId, int page, int size);

    /** Updates a branch owned by the calling tenant. */
    BranchDto updateBranch(UUID branchId, CreateBranchRequest request);

    /** Deletes a branch owned by the calling tenant. */
    void deleteBranch(UUID branchId);

    // ── Employee management ──────────────────────────────────────────────────

    /**
     * Creates an employee (STORE_MANAGER, BRANCH_MANAGER, or BRANCH_CASHIER)
     * under the calling owner's tenant, assigned to the given store.
     */
    UserDto createEmployee(UUID storeId, CreateEmployeeRequest request);

    /** Returns all employees for a store owned by the calling tenant. */
    Page<UserDto> getStoreEmployees(UUID storeId, int page, int size);

    /** Updates an employee's profile fields (name, phone, role). */
    UserDto updateEmployee(UUID employeeId, CreateEmployeeRequest request);

    /**
     * Discharges (deactivates) an employee.
     * Sets status to DISCHARGED — does not delete, history is preserved.
     */
    UserDto dischargeEmployee(UUID employeeId);
}
