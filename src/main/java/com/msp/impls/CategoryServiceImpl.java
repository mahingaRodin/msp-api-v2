package com.msp.impls;

import com.msp.enums.EUserRole;
import com.msp.exceptions.PortalException;
import com.msp.mappers.CategoryMapper;
import com.msp.models.Category;
import com.msp.models.Store;
import com.msp.models.User;
import com.msp.payloads.dtos.CategoryDto;
import com.msp.repositories.CategoryRepository;
import com.msp.repositories.StoreRepository;
import com.msp.services.CategoryService;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "categories")
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository catRepo;
    private final UserService userService;
    private final StoreRepository storeRepo;

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(key = "#result.id")
    }, evict = {
            @CacheEvict(value = "categories-by-store", allEntries = true),
            @CacheEvict(value = "categories-page",     allEntries = true)
    })
    public CategoryDto createCategory(CategoryDto categoryDto) throws Exception {
        User user = userService.getCurrentUser();

        Store store = storeRepo.findById(categoryDto.getStoreId())
                .orElseThrow(() -> new PortalException("Store not found: " + categoryDto.getStoreId()));

        assertStoreOwnership(user, store);

        Category category = Category.builder()
                .store(store)
                .name(categoryDto.getName())
                .tenantId(store.getTenantId())
                .build();

        return CategoryMapper.toDto(catRepo.save(category));
    }

    @Override
    @Cacheable(value = "categories-page", key = "#storeId + '-' + #page + '-' + #size")
    public Page<CategoryDto> getCategoriesByStore(UUID storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return catRepo.findByStoreId(storeId, pageable).map(CategoryMapper::toDto);
    }

    @Override
    @Cacheable(key = "#id")
    public CategoryDto getCategoryById(UUID id) throws Exception {
        return catRepo.findById(id)
                .map(CategoryMapper::toDto)
                .orElseThrow(() -> new PortalException("Category not found: " + id));
    }

    @Override
    @Transactional
    @Caching(put = {
            @CachePut(key = "#id")
    }, evict = {
            @CacheEvict(value = "categories-by-store", allEntries = true),
            @CacheEvict(value = "categories-page",     allEntries = true)
    })
    public CategoryDto updateCategory(UUID id, CategoryDto categoryDto) throws Exception {
        Category category = catRepo.findById(id)
                .orElseThrow(() -> new PortalException("Category not found: " + id));

        User user = userService.getCurrentUser();
        assertStoreOwnership(user, category.getStore());

        category.setName(categoryDto.getName());
        return CategoryMapper.toDto(catRepo.save(category));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(value = "categories-by-store", allEntries = true),
            @CacheEvict(value = "categories-page",     allEntries = true)
    })
    public void deleteCategory(UUID id) throws Exception {
        Category category = catRepo.findById(id)
                .orElseThrow(() -> new PortalException("Category not found: " + id));

        User user = userService.getCurrentUser();
        assertStoreOwnership(user, category.getStore());

        catRepo.delete(category);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * SUPER_ADMIN has unrestricted access.
     * STORE_ADMIN / STORE_MANAGER: tenantId must match the store's tenantId.
     * Bug fix: old code compared User objects instead of IDs.
     */
    private void assertStoreOwnership(User user, Store store) {
        if (user.getRole() == EUserRole.ROLE_SUPER_ADMIN) return;
        if (store.getTenantId() == null || !store.getTenantId().equals(user.getTenantId())) {
            throw new PortalException("Store does not belong to your business tenant.");
        }
    }
}
