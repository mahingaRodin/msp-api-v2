package com.msp.impls;

import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.PortalException;
import com.msp.models.Category;
import com.msp.models.Product;
import com.msp.models.Store;
import com.msp.models.User;
import com.msp.payloads.dtos.ProductDto;
import com.msp.repositories.CategoryRepository;
import com.msp.repositories.ProductRepository;
import com.msp.repositories.StoreRepository;
import com.msp.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Tests")
class ProductServiceImplTest {

    @Mock private ProductRepository productRepo;
    @Mock private StoreRepository storeRepo;
    @Mock private CategoryRepository catRepo;
    @Mock private UserService userService;

    @InjectMocks
    private ProductServiceImpl service;

    private UUID tenantId;
    private User storeAdmin;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        storeAdmin = new User();
        storeAdmin.setId(UUID.randomUUID());
        storeAdmin.setEmail("owner@biz.com");
        storeAdmin.setRole(EUserRole.ROLE_STORE_ADMIN);
        storeAdmin.setTenantId(tenantId);
        storeAdmin.setUserStatus(EUserStatus.ACTIVE);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Store store(UUID id) {
        Store s = new Store();
        s.setId(id);
        s.setBrand("Test Store");
        s.setTenantId(tenantId);
        s.setStoreAdmin(storeAdmin);
        return s;
    }

    private Category category(UUID id, Store s) {
        Category c = new Category();
        c.setId(id);
        c.setName("Electronics");
        c.setStore(s);
        c.setTenantId(tenantId);
        return c;
    }

    private Product product(UUID id, Store s, Category c) {
        Product p = new Product();
        p.setId(id);
        p.setName("Laptop");
        p.setSku("LAP-001");
        p.setSellingPrice(999.99);
        p.setMrp(1099.99);
        p.setStore(s);
        p.setCategory(c);
        p.setTenantId(tenantId);
        return p;
    }

    private ProductDto productDto(UUID storeId, UUID categoryId) {
        ProductDto dto = new ProductDto();
        dto.setName("Laptop");
        dto.setSku("LAP-001");
        dto.setSellingPrice(999.99);
        dto.setMrp(1099.99);
        dto.setStoreId(storeId);
        dto.setCategoryId(categoryId);
        return dto;
    }

    // ── createProduct() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProduct()")
    class CreateProduct {

        @Test
        @DisplayName("Happy path — saves product with tenantId from store")
        void create_happyPath_savesWithTenantId() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();
            Store s = store(storeId);
            Category c = category(catId, s);

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(productRepo.existsBySkuAndStoreId("LAP-001", storeId)).thenReturn(false);
            when(catRepo.findById(catId)).thenReturn(Optional.of(c));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            when(productRepo.save(captor.capture())).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            ProductDto result = service.createProduct(productDto(storeId, catId));

            assertThat(result).isNotNull();
            Product saved = captor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(tenantId);
            assertThat(saved.getStore()).isEqualTo(s);
            assertThat(saved.getCategory()).isEqualTo(c);
        }

        @Test
        @DisplayName("Throws 409 when SKU already exists in the same store")
        void create_duplicateSku_throws() {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();
            Store s = store(storeId);

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(productRepo.existsBySkuAndStoreId("LAP-001", storeId)).thenReturn(true);

            assertThatThrownBy(() -> service.createProduct(productDto(storeId, catId)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("already exists in this store");

            verify(productRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws 403 when store belongs to a different tenant")
        void create_foreignTenant_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            s.setTenantId(UUID.randomUUID());   // different tenant

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.createProduct(productDto(storeId, UUID.randomUUID())))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");

            verify(productRepo, never()).save(any());
        }

        @Test
        @DisplayName("SUPER_ADMIN can create products in any store")
        void create_superAdmin_bypasses_tenantCheck() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();
            Store s = store(storeId);
            s.setTenantId(UUID.randomUUID());   // different tenant — but super admin bypasses

            User superAdmin = new User();
            superAdmin.setRole(EUserRole.ROLE_SUPER_ADMIN);
            superAdmin.setTenantId(null);

            when(userService.getCurrentUser()).thenReturn(superAdmin);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(productRepo.existsBySkuAndStoreId(anyString(), any())).thenReturn(false);
            when(catRepo.findById(catId)).thenReturn(Optional.of(category(catId, s)));
            when(productRepo.save(any())).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            assertThatCode(() -> service.createProduct(productDto(storeId, catId)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Throws 404 when store not found")
        void create_storeNotFound_throws() {
            UUID storeId = UUID.randomUUID();
            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(storeRepo.findById(storeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createProduct(productDto(storeId, UUID.randomUUID())))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Throws 404 when category not found")
        void create_categoryNotFound_throws() {
            UUID storeId = UUID.randomUUID();
            UUID catId   = UUID.randomUUID();
            Store s = store(storeId);

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(productRepo.existsBySkuAndStoreId(anyString(), any())).thenReturn(false);
            when(catRepo.findById(catId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createProduct(productDto(storeId, catId)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── updateProduct() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProduct()")
    class UpdateProduct {

        @Test
        @DisplayName("Updates name and price for an owned product")
        void update_ownedProduct_succeeds() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID catId    = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Category c = category(catId, s);
            Product p = product(productId, s, c);

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));
            when(productRepo.save(any())).thenReturn(p);

            ProductDto dto = new ProductDto();
            dto.setName("Gaming Laptop");
            dto.setSellingPrice(1299.99);

            service.updateProduct(productId, dto);

            verify(productRepo).save(argThat(saved ->
                    saved.getName().equals("Gaming Laptop")
                    && saved.getSellingPrice().equals(1299.99)));
        }

        @Test
        @DisplayName("SKU change succeeds when new SKU is unique in the store")
        void update_skuChange_unique_succeeds() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));
            when(productRepo.existsBySkuAndStoreId("NEW-SKU", storeId)).thenReturn(false);
            when(productRepo.save(any())).thenReturn(p);

            ProductDto dto = new ProductDto();
            dto.setSku("NEW-SKU");

            assertThatCode(() -> service.updateProduct(productId, dto))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SKU change throws when new SKU already taken in the store")
        void update_skuChange_duplicate_throws() {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));
            when(productRepo.existsBySkuAndStoreId("TAKEN-SKU", storeId)).thenReturn(true);

            ProductDto dto = new ProductDto();
            dto.setSku("TAKEN-SKU");

            assertThatThrownBy(() -> service.updateProduct(productId, dto))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("already exists in this store");
        }

        @Test
        @DisplayName("Setting same SKU does not trigger uniqueness check")
        void update_sameSku_noCheck() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Product p = product(productId, s, category(UUID.randomUUID(), s));  // sku = "LAP-001"

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));
            when(productRepo.save(any())).thenReturn(p);

            ProductDto dto = new ProductDto();
            dto.setSku("LAP-001");   // same as existing

            service.updateProduct(productId, dto);

            verify(productRepo, never()).existsBySkuAndStoreId(anyString(), any());
        }

        @Test
        @DisplayName("Throws 403 when product belongs to a different tenant")
        void update_foreignTenant_throws() {
            UUID productId = UUID.randomUUID();
            Store s = store(UUID.randomUUID());
            s.setTenantId(UUID.randomUUID());   // different tenant
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.updateProduct(productId, new ProductDto()))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ── deleteProduct() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteProduct()")
    class DeleteProduct {

        @Test
        @DisplayName("Deletes an owned product")
        void delete_ownedProduct_succeeds() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));

            service.deleteProduct(productId);

            verify(productRepo).delete(p);
        }

        @Test
        @DisplayName("Throws 403 when product belongs to a different tenant")
        void delete_foreignTenant_throws() {
            UUID productId = UUID.randomUUID();
            Store s = store(UUID.randomUUID());
            s.setTenantId(UUID.randomUUID());
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.deleteProduct(productId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");

            verify(productRepo, never()).delete(any());
        }

        @Test
        @DisplayName("Throws 404 when product not found")
        void delete_notFound_throws() {
            UUID productId = UUID.randomUUID();
            when(userService.getCurrentUser()).thenReturn(storeAdmin);
            when(productRepo.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteProduct(productId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── getProductById() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProductById()")
    class GetProductById {

        @Test
        @DisplayName("Returns DTO for existing product")
        void get_exists_returnsDto() throws Exception {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(productRepo.findById(productId)).thenReturn(Optional.of(p));

            ProductDto result = service.getProductById(productId);

            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo("Laptop");
        }

        @Test
        @DisplayName("Throws 404 for non-existent product")
        void get_notFound_throws() {
            UUID productId = UUID.randomUUID();
            when(productRepo.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProductById(productId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── getProductsByStoreId() ────────────────────────────────────────────────

    @Nested
    @DisplayName("getProductsByStoreId()")
    class GetProductsByStore {

        @Test
        @DisplayName("Returns paginated products for a store")
        void list_returnsPage() {
            UUID storeId   = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Store s = store(storeId);
            Product p = product(productId, s, category(UUID.randomUUID(), s));

            when(productRepo.findByStoreId(eq(storeId), any()))
                    .thenReturn(new PageImpl<>(List.of(p)));

            Page<ProductDto> result = service.getProductsByStoreId(storeId, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(productId);
        }
    }
}
