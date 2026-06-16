package com.msp.controllers;

import com.msp.payloads.dtos.ProductDto;
import com.msp.payloads.response.ApiResponse2;
import com.msp.services.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Product catalog — public browsing and tenant-scoped management")
public class ProductController {

    private final ProductService productService;

    // ── Public browsing (no auth required) ───────────────────────────────────

    @Operation(summary = "Browse all products for a store",
               description = "Public endpoint — customers and guests can browse a store's product catalog.")
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.getProductsByStoreId(storeId, page, size));
    }

    @Operation(summary = "Search products in a store",
               description = "Public endpoint — search by name, brand, or SKU.")
    @GetMapping("/store/{storeId}/search")
    public ResponseEntity<Page<ProductDto>> searchByKeyword(
            @PathVariable UUID storeId,
            @Parameter(description = "Search keyword (name, brand, or SKU)")
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.searchByKeyword(storeId, keyword, page, size));
    }

    @Operation(summary = "Get a single product by ID",
               description = "Public endpoint — returns full product details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable UUID id) throws Exception {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ── Tenant-scoped management (store staff only) ───────────────────────────

    @Operation(summary = "Create a product",
               description = "Creates a product in the authenticated user's tenant store. " +
                             "SKU must be unique within the store.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Store or category not found"),
            @ApiResponse(responseCode = "409", description = "SKU already exists in this store")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductDto productDto) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(productDto));
    }

    @Operation(summary = "Update a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "SKU already exists in this store")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_BRANCH_MANAGER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductDto productDto) throws Exception {
        return ResponseEntity.ok(productService.updateProduct(id, productDto));
    }

    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product deleted"),
            @ApiResponse(responseCode = "403", description = "Store belongs to a different tenant"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_STORE_ADMIN','ROLE_STORE_MANAGER','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse2> deleteProduct(@PathVariable UUID id) throws Exception {
        productService.deleteProduct(id);
        ApiResponse2 res = new ApiResponse2();
        res.setMessage("Product deleted successfully.");
        return ResponseEntity.ok(res);
    }
}
