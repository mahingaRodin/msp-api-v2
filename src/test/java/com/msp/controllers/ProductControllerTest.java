package com.msp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.msp.exceptions.PortalException;
import com.msp.payloads.dtos.ProductDto;
import com.msp.services.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private ProductService productService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private ProductDto productDto(UUID id, UUID storeId) {
        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setName("Laptop");
        dto.setSku("LAP-001");
        dto.setSellingPrice(999.99);
        dto.setMrp(1099.99);
        dto.setStoreId(storeId);
        return dto;
    }

    private ProductDto createRequest(UUID storeId, UUID categoryId) {
        ProductDto dto = new ProductDto();
        dto.setName("Laptop");
        dto.setSku("LAP-001");
        dto.setSellingPrice(999.99);
        dto.setMrp(1099.99);
        dto.setStoreId(storeId);
        dto.setCategoryId(categoryId);
        return dto;
    }

    // ── GET /api/products/store/{storeId} — Public ────────────────────────────

    @Nested
    @DisplayName("GET /api/products/store/{storeId} — Browse Products (Public)")
    class BrowseProducts {

        @Test
        @DisplayName("Returns paginated product list — no auth required")
        void browse_returnsPage() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Page<ProductDto> page = new PageImpl<>(List.of(productDto(productId, storeId)));

            Mockito.when(productService.getProductsByStoreId(eq(storeId), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/products/store/{storeId}", storeId)
                            .param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(productId.toString()))
                    .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Empty store returns 200 with empty content")
        void browse_empty_returnsEmptyPage() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(productService.getProductsByStoreId(eq(storeId), anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/products/store/{storeId}", storeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── GET /api/products/store/{storeId}/search — Public ────────────────────

    @Nested
    @DisplayName("GET /api/products/store/{storeId}/search — Search Products (Public)")
    class SearchProducts {

        @Test
        @DisplayName("Returns matching products for keyword")
        void search_returnsMatches() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Page<ProductDto> page = new PageImpl<>(List.of(productDto(productId, storeId)));

            Mockito.when(productService.searchByKeyword(eq(storeId), eq("laptop"), anyInt(), anyInt()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/products/store/{storeId}/search", storeId)
                            .param("keyword", "laptop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Laptop"));
        }

        @Test
        @DisplayName("No matches returns 200 with empty content")
        void search_noMatches_returnsEmpty() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(productService.searchByKeyword(eq(storeId), eq("xyz"), anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/products/store/{storeId}/search", storeId)
                            .param("keyword", "xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── GET /api/products/{id} — Public ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/products/{id} — Get Product By ID (Public)")
    class GetProductById {

        @Test
        @DisplayName("Existing product returns 200 with full details")
        void get_exists_returns200() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Mockito.when(productService.getProductById(productId))
                    .thenReturn(productDto(productId, storeId));

            mockMvc.perform(get("/api/products/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productId.toString()))
                    .andExpect(jsonPath("$.sku").value("LAP-001"));
        }

        @Test
        @DisplayName("Non-existent product returns 404")
        void get_notFound_returns404() throws Exception {
            UUID productId = UUID.randomUUID();
            Mockito.when(productService.getProductById(productId))
                    .thenThrow(new PortalException("Product not found: " + productId));

            mockMvc.perform(get("/api/products/{id}", productId))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/products — Create ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/products — Create Product")
    class CreateProduct {

        @Test
        @DisplayName("Valid request returns 201 with product data")
        void create_valid_returns201() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID catId     = UUID.randomUUID();

            Mockito.when(productService.createProduct(any(ProductDto.class)))
                    .thenReturn(productDto(productId, storeId));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest(storeId, catId))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(productId.toString()))
                    .andExpect(jsonPath("$.name").value("Laptop"));
        }

        @Test
        @DisplayName("Duplicate SKU in store returns 409")
        void create_duplicateSku_returns409() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();

            Mockito.when(productService.createProduct(any()))
                    .thenThrow(new PortalException(
                            "A product with SKU 'LAP-001' already exists in this store."));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest(storeId, catId))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Store belongs to different tenant returns 403")
        void create_foreignTenant_returns403() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();

            Mockito.when(productService.createProduct(any()))
                    .thenThrow(new PortalException("Store does not belong to your business tenant."));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest(storeId, catId))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Store not found returns 404")
        void create_storeNotFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();

            Mockito.when(productService.createProduct(any()))
                    .thenThrow(new PortalException("Store not found: " + storeId));

            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest(storeId, catId))))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/products/{id} — Update ────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/products/{id} — Update Product")
    class UpdateProduct {

        @Test
        @DisplayName("Valid update returns 200 with updated data")
        void update_valid_returns200() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            ProductDto updated = productDto(productId, storeId);
            updated.setName("Gaming Laptop");

            Mockito.when(productService.updateProduct(eq(productId), any()))
                    .thenReturn(updated);

            ProductDto req = new ProductDto();
            req.setName("Gaming Laptop");

            mockMvc.perform(patch("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Gaming Laptop"));
        }

        @Test
        @DisplayName("Product not found returns 404")
        void update_notFound_returns404() throws Exception {
            UUID productId = UUID.randomUUID();
            Mockito.when(productService.updateProduct(eq(productId), any()))
                    .thenThrow(new PortalException("Product not found: " + productId));

            mockMvc.perform(patch("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Duplicate SKU returns 409")
        void update_duplicateSku_returns409() throws Exception {
            UUID productId = UUID.randomUUID();
            Mockito.when(productService.updateProduct(eq(productId), any()))
                    .thenThrow(new PortalException(
                            "A product with SKU 'TAKEN' already exists in this store."));

            mockMvc.perform(patch("/api/products/{id}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sku\":\"TAKEN\"}"))
                    .andExpect(status().isConflict());
        }
    }

    // ── DELETE /api/products/{id} — Delete ───────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/products/{id} — Delete Product")
    class DeleteProduct {

        @Test
        @DisplayName("Existing product returns 200 with message")
        void delete_exists_returns200() throws Exception {
            UUID productId = UUID.randomUUID();
            Mockito.doNothing().when(productService).deleteProduct(productId);

            mockMvc.perform(delete("/api/products/{id}", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Product deleted successfully."));
        }

        @Test
        @DisplayName("Product not found returns 404")
        void delete_notFound_returns404() throws Exception {
            UUID productId = UUID.randomUUID();
            Mockito.doThrow(new PortalException("Product not found: " + productId))
                    .when(productService).deleteProduct(productId);

            mockMvc.perform(delete("/api/products/{id}", productId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Product belongs to different tenant returns 403")
        void delete_foreignTenant_returns403() throws Exception {
            UUID productId = UUID.randomUUID();
            Mockito.doThrow(new PortalException("Store does not belong to your business tenant."))
                    .when(productService).deleteProduct(productId);

            mockMvc.perform(delete("/api/products/{id}", productId))
                    .andExpect(status().isForbidden());
        }
    }
}
