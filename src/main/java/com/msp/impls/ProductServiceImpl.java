package com.msp.impls;

import com.msp.exceptions.PortalException;
import com.msp.mappers.ProductMapper;
import com.msp.models.Category;
import com.msp.models.Product;
import com.msp.models.Store;
import com.msp.models.User;
import com.msp.payloads.dtos.ProductDto;
import com.msp.repositories.CategoryRepository;
import com.msp.repositories.ProductRepository;
import com.msp.repositories.StoreRepository;
import com.msp.services.ProductService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "products")
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final StoreRepository storeRepo;
    private final CategoryRepository catRepo;
    private final UserService userService;

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(key = "#result.id")
    }, evict = {
            @CacheEvict(value = "products-by-store", allEntries = true),
            @CacheEvict(value = "products-search",   allEntries = true)
    })
    public ProductDto createProduct(ProductDto productDto) throws Exception {
        User currentUser = userService.getCurrentUser();

        Store store = storeRepo.findById(productDto.getStoreId())
                .orElseThrow(() -> new PortalException("Store not found: " + productDto.getStoreId()));

        // Tenant ownership check — only the store's tenant can add products to it
        assertStoreOwnership(currentUser, store);

        // SKU uniqueness is per-store
        if (productDto.getSku() != null
                && productRepo.existsBySkuAndStoreId(productDto.getSku(), store.getId())) {
            throw new PortalException(
                    "A product with SKU '" + productDto.getSku() + "' already exists in this store.");
        }

        Category category = catRepo.findById(productDto.getCategoryId())
                .orElseThrow(() -> new PortalException("Category not found: " + productDto.getCategoryId()));

        Product product = ProductMapper.toEntity(productDto, store, category);
        product.setTenantId(store.getTenantId());

        return ProductMapper.toDto(productRepo.save(product));
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(key = "#id")
    }, evict = {
            @CacheEvict(value = "products-by-store", allEntries = true),
            @CacheEvict(value = "products-search",   allEntries = true)
    })
    public ProductDto updateProduct(UUID id, ProductDto productDto) throws Exception {
        User currentUser = userService.getCurrentUser();

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new PortalException("Product not found: " + id));

        assertStoreOwnership(currentUser, product.getStore());

        if (productDto.getCategoryId() != null) {
            Category category = catRepo.findById(productDto.getCategoryId())
                    .orElseThrow(() -> new PortalException("Category not found: " + productDto.getCategoryId()));
            product.setCategory(category);
        }
        if (productDto.getName()         != null) product.setName(productDto.getName());
        if (productDto.getDescription()  != null) product.setDescription(productDto.getDescription());
        if (productDto.getSku()          != null) {
            // Only check uniqueness if SKU is actually changing
            if (!productDto.getSku().equals(product.getSku())
                    && productRepo.existsBySkuAndStoreId(productDto.getSku(), product.getStore().getId())) {
                throw new PortalException(
                        "A product with SKU '" + productDto.getSku() + "' already exists in this store.");
            }
            product.setSku(productDto.getSku());
        }
        if (productDto.getImage()        != null) product.setImage(productDto.getImage());
        if (productDto.getMrp()          != null) product.setMrp(productDto.getMrp());
        if (productDto.getSellingPrice() != null) product.setSellingPrice(productDto.getSellingPrice());
        if (productDto.getBrand()        != null) product.setBrand(productDto.getBrand());

        product.setUpdatedAt(LocalDateTime.now());
        return ProductMapper.toDto(productRepo.save(product));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(value = "products-by-store", allEntries = true),
            @CacheEvict(value = "products-search",   allEntries = true)
    })
    public void deleteProduct(UUID id) throws Exception {
        User currentUser = userService.getCurrentUser();
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new PortalException("Product not found: " + id));
        assertStoreOwnership(currentUser, product.getStore());
        productRepo.delete(product);
    }

    @Override
    @Cacheable(key = "#id")
    public ProductDto getProductById(UUID id) throws Exception {
        return productRepo.findById(id)
                .map(ProductMapper::toDto)
                .orElseThrow(() -> new PortalException("Product not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products-by-store", key = "#storeId + '-' + #page + '-' + #size")
    public Page<ProductDto> getProductsByStoreId(UUID storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepo.findByStoreId(storeId, pageable).map(ProductMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products-search", key = "#storeId + '-' + #keyword + '-' + #page + '-' + #size")
    public Page<ProductDto> searchByKeyword(UUID storeId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepo.searchByKeyword(storeId, keyword, pageable).map(ProductMapper::toDto);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Allows SUPER_ADMIN unrestricted access.
     * For STORE_ADMIN and STORE_MANAGER: the user's tenantId must match the store's tenantId.
     */
    private void assertStoreOwnership(User user, Store store) {
        if (user.getRole() == com.msp.enums.EUserRole.ROLE_SUPER_ADMIN) return;
        if (store.getTenantId() == null || !store.getTenantId().equals(user.getTenantId())) {
            throw new PortalException("Store does not belong to your business tenant.");
        }
    }
}
