package com.msp.controllers;

import com.msp.payloads.dtos.BranchDto;
import com.msp.payloads.response.ApiResponse2;
import com.msp.services.BranchService;
import com.msp.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/branches")
@Tag(name = "Branch Management", description = "Endpoints for managing branches in the system")
@SecurityRequirement(name = "Bearer Authentication")
public class BranchController {
    private final BranchService branchService;
    private final UserService userService;

    @Operation(
            summary = "Create a new branch",
            description = "Creates a new branch with the provided details. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Branch created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BranchDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Branch already exists with the given details",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PostMapping
    public ResponseEntity<BranchDto> createBranch(
            @Parameter(
                    description = "Branch details to create",
                    required = true,
                    schema = @Schema(implementation = BranchDto.class)
            )
            @Valid @RequestBody BranchDto branchDto
    ) throws Exception {
        BranchDto createdBranch = branchService.createBranch(branchDto);
        return ResponseEntity.ok(createdBranch);
    }

    @Operation(
            summary = "Get all branches by store ID",
            description = "Retrieves a list of all branches associated with a specific store ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved branches",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = BranchDto.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid store ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Store not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Page<BranchDto>> getAllBranchesByStoreId(
            @Parameter(
                    name = "storeId",
                    description = "UUID of the store to retrieve branches for",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10")int size
    ) throws Exception {
        Page<BranchDto> branches = branchService.getAllBranchesByStoreId(storeId,page,size);
        return ResponseEntity.ok(branches);
    }

    @Operation(
            summary = "Update an existing branch",
            description = "Updates the details of an existing branch identified by its ID. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Branch updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BranchDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or branch ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Branch not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Update conflicts with existing data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_STORE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BranchDto> updateBranch(
            @Parameter(
                    name = "id",
                    description = "UUID of the branch to update",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id,

            @Parameter(
                    description = "Updated branch details",
                    required = true,
                    schema = @Schema(implementation = BranchDto.class)
            )
            @Valid @RequestBody BranchDto branchDto
    ) throws Exception {
        BranchDto updatedBranch = branchService.updateBranch(id, branchDto);
        return ResponseEntity.ok(updatedBranch);
    }

    @Operation(
            summary = "Delete a branch",
            description = "Deletes an existing branch identified by its ID. Requires ADMIN role only."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Branch deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid branch ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Branch not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot delete branch with existing dependencies",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_STORE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse2> deleteBranch(
            @Parameter(
                    name = "id",
                    description = "UUID of the branch to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id
    ) throws Exception {
        ApiResponse2 apiResponse = new ApiResponse2();
        apiResponse.setMessage("Branch deleted Successfully!");
        branchService.deleteBranch(id);
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "Get all branches",
            description = "Retrieves a paginated list of all branches"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Branches retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<BranchDto>> getAllBranches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size
    ) throws Exception {
        Page<BranchDto> branches = branchService.getAllBranches(page, size);
        return ResponseEntity.ok(branches);
    }
}