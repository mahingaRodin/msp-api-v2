package com.msp.controllers;

import com.msp.enums.EStoreStatus;
import com.msp.mappers.StoreMapper;
import com.msp.models.User;
import com.msp.payloads.dtos.StoreDto;
import com.msp.payloads.response.ApiResponse2;
import com.msp.services.StoreService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
@Tag(name = "Store Management", description = "Endpoints for managing stores in the system")
@SecurityRequirement(name = "Bearer Authentication")
public class StoreController {
        private final StoreService storeService;
        private final UserService userService;

        @Operation(summary = "Create a new store", description = "Creates a new store with the provided details. Requires ADMIN role only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data - Store name or email may already exist", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "409", description = "Store with this name or email already exists", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN')")
        @PostMapping
        public ResponseEntity<StoreDto> createStore(
                        @Parameter(description = "Store details to create", required = true, schema = @Schema(implementation = StoreDto.class)) @Valid @RequestBody StoreDto storeDto,

                        @Parameter(description = "Bearer JWT token for authentication", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", schema = @Schema(type = "string", format = "Bearer [token]")) @RequestHeader("Authorization") String token)
                        throws Exception {
                User user = userService.getCurrentUserFromToken(token);
                return ResponseEntity.ok(storeService.createStore(storeDto, user));
        }

        @Operation(summary = "Get store by ID", description = "Retrieves detailed information of a specific store by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid store ID format", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Store not found with the given ID", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
        public ResponseEntity<StoreDto> getStoreById(
                        @Parameter(name = "id", description = "UUID of the store to retrieve", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID id,

                        @Parameter(description = "Bearer JWT token for authentication", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", schema = @Schema(type = "string", format = "Bearer [token]")) @RequestHeader("Authorization") String token)
                        throws Exception {
                return ResponseEntity.ok(storeService.getStoreById(id));
        }

        @Operation(summary = "Get all stores", description = "Retrieves a list of all stores in the system. Requires ADMIN or MANAGER role.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Stores retrieved successfully", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = StoreDto.class)))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN or MANAGER role required", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN or MANAGER role required", content = @Content) })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_BRANCH_MANAGER','ROLE_STORE_MANAGER','ROLE_CUSTOMER')")
        @GetMapping
        public ResponseEntity<Page<StoreDto>> getAllStores(
                        @Parameter(name = "page", description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,

                        @Parameter(name = "size", description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,

                        @Parameter(name = "direction", description = "Sort direction", example = "DESC", schema = @Schema(allowableValues = {
                                        "ASC", "DESC" })) @RequestParam(defaultValue = "DESC") String direction) {
                Sort sort = Sort.by(Sort.Direction.fromString(direction), "createdAt");
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<StoreDto> stores = storeService.getAllStores(pageable);
                return ResponseEntity.ok(stores);
        }

        @Operation(summary = "Get store by admin", description = "Retrieves the store associated with the currently authenticated admin user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDto.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "No store found for the current admin", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_BRANCH_MANAGER','ROLE_STORE_MANAGER')")
        @GetMapping("/admin")
        public ResponseEntity<StoreDto> getStoreByAdmin(
                        @Parameter(description = "Bearer JWT token for authentication", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", schema = @Schema(type = "string", format = "Bearer [token]")) @RequestHeader("Authorization") String token)
                        throws Exception {
                return ResponseEntity.ok(StoreMapper.toDto(storeService.getStoreByAdmin()));
        }

        @Operation(summary = "Get store by employee", description = "Retrieves the store where the currently authenticated employee works")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDto.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Employee role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "No store found for the current employee", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_BRANCH_MANAGER','ROLE_STORE_MANAGER')")
        @GetMapping("/employee")
        public ResponseEntity<StoreDto> getStoreByEmployee(
                        @Parameter(description = "Bearer JWT token for authentication", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", schema = @Schema(type = "string", format = "Bearer [token]")) @RequestHeader("Authorization") String token)
                        throws Exception {
                return ResponseEntity.ok(storeService.getStoreByEmployee());
        }

        @Operation(summary = "Update store details", description = "Updates general information of an existing store. Requires ADMIN or STORE_MANAGER role.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data or store ID format", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN or STORE_MANAGER role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Store not found with the given ID", content = @Content),
                        @ApiResponse(responseCode = "409", description = "Store name or email already in use by another store", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_STORE_MANAGER')")
        @PutMapping("/{id}/update")
        public ResponseEntity<StoreDto> updateStore(
                        @Parameter(name = "id", description = "UUID of the store to update", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID id,

                        @Parameter(description = "Updated store details", required = true, schema = @Schema(implementation = StoreDto.class)) @Valid @RequestBody StoreDto storeDto)
                        throws Exception {
                return ResponseEntity.ok(storeService.updateStore(id, storeDto));
        }

        @Operation(summary = "Moderate store status", description = "Updates the moderation status of a store (ACTIVE, SUSPENDED, CLOSED, etc.). Requires ADMIN role only.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store status updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreDto.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid store ID format or status value", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Store not found with the given ID", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_STORE_MANAGER')")
        @PutMapping("/{id}/moderate")
        public ResponseEntity<StoreDto> moderateStore(
                        @Parameter(name = "id", description = "UUID of the store to moderate", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID id,

                        @Parameter(name = "status", description = "New moderation status for the store", required = true, example = "ACTIVE", schema = @Schema(implementation = EStoreStatus.class)) @RequestParam EStoreStatus status)
                        throws Exception {
                return ResponseEntity.ok(storeService.moderateStore(id, status));
        }

        @Operation(summary = "Delete a store", description = "Deletes an existing store by its ID. Requires ADMIN role only. This will also cascade delete associated branches and employees.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Store deleted successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse2.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid store ID format", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Store not found with the given ID", content = @Content),
                        @ApiResponse(responseCode = "409", description = "Cannot delete store with active branches or employees", content = @Content),
                        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
        })
        @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_STORE_MANAGER')")
        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse2> deleteStore(
                        @Parameter(name = "id", description = "UUID of the store to delete", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID id)
                        throws Exception {
                storeService.deleteStore(id);
                ApiResponse2 response = new ApiResponse2();
                response.setMessage("Store Deleted Successfully!");
                return ResponseEntity.ok(response);
        }
}