package com.msp.controllers;

import com.msp.payloads.dtos.CategoryDto;
import com.msp.payloads.response.ApiResponse2;
import com.msp.services.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Endpoints for managing product categories")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "Create a new category",
            description = "Creates a new product category with the provided details. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Category created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CategoryDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data - Category name may already exist or validation failed",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions (ADMIN or MANAGER required)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Store not found for the given store ID",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_STORE_ADMIN','ROLE_STORE_MANAGER')")
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(
            @Parameter(
                    description = "Category details to create",
                    required = true,
                    schema = @Schema(implementation = CategoryDto.class)
            )
            @Valid @RequestBody CategoryDto categoryDto
    ) throws Exception {
        return ResponseEntity.ok(categoryService.createCategory(categoryDto));
    }

    @Operation(
            summary = "Get categories by store ID",
            description = "Retrieves a list of all categories associated with a specific store"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Categories retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CategoryDto.class))
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
    public ResponseEntity<Page<CategoryDto>> getCategoriesByStoreId(
            @Parameter(
                    name = "storeId",
                    description = "UUID of the store to retrieve categories for",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID storeId,
            @RequestParam(defaultValue="0")int page,
            @RequestParam(defaultValue = "10") int size
    ) throws Exception {
        return ResponseEntity.ok(
                categoryService.getCategoriesByStore(storeId,page,size)
        );
    }

    @Operation(summary = "Get a single category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable UUID id) throws Exception {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @Operation(
            summary = "Update an existing category",
            description = "Updates the details of an existing category identified by its ID. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Category updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CategoryDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or category ID format",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Insufficient permissions (ADMIN or MANAGER required)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found with the given ID",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category name already exists in the same store",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_STORE_MANAGER','ROLE_STORE_ADMIN')")
    public ResponseEntity<CategoryDto> updateCategory(
            @Parameter(
                    description = "Updated category details",
                    required = true,
                    schema = @Schema(implementation = CategoryDto.class)
            )
            @Valid @RequestBody CategoryDto categoryDto,

            @Parameter(
                    name = "id",
                    description = "UUID of the category to update",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id
    ) throws Exception {
        return ResponseEntity.ok(
                categoryService.updateCategory(id, categoryDto)
        );
    }

    @Operation(
            summary = "Delete a category",
            description = "Deletes an existing category identified by its ID. Requires ADMIN role only."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Category deleted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse2.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid category ID format",
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
                    description = "Category not found with the given ID",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Cannot delete category with associated products",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN','ROLE_SUPER_ADMIN','ROLE_STORE_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse2> deleteCategory(
            @Parameter(
                    name = "id",
                    description = "UUID of the category to delete",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable UUID id
    ) throws Exception {
        categoryService.deleteCategory(id);
        ApiResponse2 apiResponse = new ApiResponse2();
        apiResponse.setMessage("Category Deleted Successfully!");
        return ResponseEntity.ok(apiResponse);
    }
}