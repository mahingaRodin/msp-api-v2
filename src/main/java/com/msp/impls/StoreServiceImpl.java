package com.msp.impls;

import com.msp.enums.EStoreStatus;
import com.msp.exceptions.UserException;
import com.msp.mappers.StoreMapper;
import com.msp.models.Store;
import com.msp.models.StoreContact;
import com.msp.models.User;
import com.msp.payloads.dtos.StoreDto;
import com.msp.repositories.StoreRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.StoreService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "stores")
public class StoreServiceImpl implements StoreService {
        private final StoreRepository storeRepo;
        private final UserRepository userRepo;
        private final UserService userService;

        @Override
        @Caching(put = {
                        @CachePut(key = "#result.id")
        }, evict = {
                        @CacheEvict(value = "stores-all", allEntries = true),
                        @CacheEvict(value = "stores-by-admin", allEntries = true),
                        @CacheEvict(value = "stores-by-employee", allEntries = true)
        })
        public StoreDto createStore(StoreDto storeDto, User user) {
                Store store = StoreMapper.toEntity(storeDto, user);
                Store saved = storeRepo.save(store);
                if (user.getStore() == null) {
                        user.setStore(saved);
                        if (user.getTenantId() == null && saved.getTenantId() != null) {
                                user.setTenantId(saved.getTenantId());
                        }
                        userRepo.save(user);
                }
                return StoreMapper.toDto(saved);
        }

        @Override
        @Cacheable(key = "#id")
        public StoreDto getStoreById(UUID id) throws Exception {
                Store store = storeRepo.findById(id).orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store Not Found!"));
                return StoreMapper.toDto(store);
        }

        @Override
        @Cacheable(value = "stores-all", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
        public Page<StoreDto> getAllStores(Pageable pageable) {
                Page<Store> dtos = storeRepo.findAll(pageable);
                return dtos.map(StoreMapper::toDto);
        }

        @Override
        public Store getStoreByAdmin() {
                User admin = userService.getCurrentUser();
                Store store = storeRepo.findByStoreAdminId(admin.getId());
                if (store == null) {
                        store = admin.getStore();
                }
                if (store == null) {
                        throw new UserException("No store is linked to your account yet. Create a store first.");
                }
                return store;
        }

        @Override
        @Caching(put = {
                        @CachePut(key = "#id")
        }, evict = {
                        @CacheEvict(value = "stores-all", allEntries = true),
                        @CacheEvict(value = "stores-by-admin", allEntries = true),
                        @CacheEvict(value = "stores-by-employee", allEntries = true)
        })
        public StoreDto updateStore(UUID id, StoreDto storeDto) throws Exception {
                User currentUser = userService.getCurrentUser();
                Store existing = storeRepo.findById(id).orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store Not Found!"));

                // Permission check: current user must be either super_admin or the specific
                // store_admin
                boolean isSuperAdmin = currentUser.getRole() == com.msp.enums.EUserRole.ROLE_SUPER_ADMIN;

                if (!isSuperAdmin && !existing.getStoreAdmin().getId().equals(currentUser.getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to update this store!");
                }

                existing.setBrand(storeDto.getBrand());
                existing.setDescription(storeDto.getDescription());

                if (storeDto.getStoreType() != null) {
                        existing.setStoreType(storeDto.getStoreType());
                }
                if (storeDto.getContact() != null) {
                        StoreContact contact = StoreContact.builder()
                                        .address(storeDto.getContact().getAddress())
                                        .phone(storeDto.getContact().getPhone())
                                        .email(storeDto.getContact().getEmail())
                                        .build();
                        existing.setContact(contact);
                }
                Store updatedStore = storeRepo.save(existing);
                return StoreMapper.toDto(updatedStore);
        }

        @Override
        @Caching(evict = {
                        @CacheEvict(key = "#id"),
                        @CacheEvict(value = "stores-all", allEntries = true),
                        @CacheEvict(value = "stores-by-admin", allEntries = true),
                        @CacheEvict(value = "stores-by-employee", allEntries = true)
        })
        public void deleteStore(UUID id) throws Exception {
                User currentUser = userService.getCurrentUser();
                Store store = storeRepo.findById(id).orElseThrow(
                                () -> new Exception("Store Not Found!"));

                boolean isSuperAdmin = currentUser.getRole() == com.msp.enums.EUserRole.ROLE_SUPER_ADMIN;

                if (!isSuperAdmin && !store.getStoreAdmin().getId().equals(currentUser.getId())) {
                        throw new Exception("You don't have permission to delete this store!");
                }

                storeRepo.delete(store);
        }

        @Override
        public StoreDto getStoreByEmployee() {
                User currentUser = userService.getCurrentUser();
                if (currentUser == null) {
                        throw new UserException("You don't have permission to access this store!");
                }
                return StoreMapper.toDto(currentUser.getStore());
        }

        @Override
        @Caching(put = {
                        @CachePut(key = "#id")
        }, evict = {
                        @CacheEvict(value = "stores-all", allEntries = true),
                        @CacheEvict(value = "stores-by-admin", allEntries = true),
                        @CacheEvict(value = "stores-by-employee", allEntries = true)
        })
        public StoreDto moderateStore(UUID id, EStoreStatus status) throws Exception {
                Store store = storeRepo.findById(id).orElseThrow(
                                () -> new Exception("Store Not Found!"));
                store.setStatus(status);
                Store updatedStore = storeRepo.save(store);
                return StoreMapper.toDto(updatedStore);
        }
}