package com.msp.impls;

import com.msp.mappers.BranchMapper;
import com.msp.models.Branch;
import com.msp.models.Store;
import com.msp.models.User;
import com.msp.payloads.dtos.BranchDto;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.StoreRepository;
import com.msp.services.BranchService;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "branches")
public class BranchServiceImpl implements BranchService {
        private final BranchRepository branchRepo;
        private final StoreRepository storeRepo;
        private final UserService userService;

        @Override
        @Caching(put = {
                        @CachePut(key = "#result.id")
        }, evict = {
                        @CacheEvict(value = "branches-by-store", allEntries = true),
                        @CacheEvict(value = "branches-page", allEntries = true)
        })
        public BranchDto createBranch(BranchDto branchDto) {
                User currentUser = userService.getCurrentUser();
                Store store;

                boolean isSuperAdmin = currentUser.getRole() == com.msp.enums.EUserRole.ROLE_SUPER_ADMIN;

                if (isSuperAdmin && branchDto.getStoreId() != null) {
                        store = storeRepo.findById(branchDto.getStoreId()).orElse(null);
                } else {
                        store = storeRepo.findByStoreAdminId(currentUser.getId());
                }

                Branch branch = BranchMapper.toEntity(branchDto, store);
                Branch savedBranch = branchRepo.save(branch);
                return BranchMapper.toDto(savedBranch);
        }

        @Override
        @Caching(put = {
                        @CachePut(key = "#id")
        }, evict = {
                        @CacheEvict(value = "branches-by-store", allEntries = true),
                        @CacheEvict(value = "branches-page", allEntries = true)
        })
        public BranchDto updateBranch(UUID id, BranchDto branchDto) throws Exception {
                Branch existing = branchRepo.findById(id).orElseThrow(
                                () -> new Exception("Branch doesn't exist..."));

                existing.setName(branchDto.getName());
                existing.setWorkingDays(branchDto.getWorkingDays());
                existing.setEmail(branchDto.getEmail());
                existing.setAddress(branchDto.getAddress());
                existing.setPhone(branchDto.getPhone());
                existing.setOpenTime(branchDto.getOpenTime());
                existing.setCloseTime(branchDto.getCloseTime());
                existing.setUpdatedAt(LocalDateTime.now());

                Branch updatedBranch = branchRepo.save(existing);
                return BranchMapper.toDto(updatedBranch);
        }

        @Override
        @Caching(evict = {
                        @CacheEvict(key = "#id"),
                        @CacheEvict(value = "branches-by-store", allEntries = true),
                        @CacheEvict(value = "branches-page", allEntries = true)
        })
        public void deleteBranch(UUID id) throws Exception {
                Branch existing = branchRepo.findById(id).orElseThrow(
                                () -> new Exception("Branch doesn't exist..."));
                branchRepo.delete(existing);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(value = "branches-page", key = "#storeId + '-' + #page + '-' + #size")
        public Page<BranchDto> getAllBranchesByStoreId(UUID storeId, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                return branchRepo.findByStoreId(storeId, pageable).map(BranchMapper::toDto);
        }

        @Override
        @Transactional(readOnly = true)
        @Cacheable(key = "#id")
        public BranchDto getBranchById(UUID id) throws Exception {
                Branch existing = branchRepo.findById(id).orElseThrow(
                                () -> new Exception("Branch doesn't exist..."));
                return BranchMapper.toDto(existing);
        }

        @Override
        public Page<BranchDto> getAllBranches(int page, int size) {
                Pageable pageable = PageRequest.of(page,size, Sort.by("createdAt").descending());
                return branchRepo.findAll(pageable).map(BranchMapper::toDto);
        }
}