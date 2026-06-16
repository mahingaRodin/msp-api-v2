package com.msp.impls;

import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.PortalException;
import com.msp.mappers.BranchMapper;
import com.msp.mappers.StoreMapper;
import com.msp.mappers.UserMapper;
import com.msp.models.Branch;
import com.msp.models.Store;
import com.msp.models.StoreContact;
import com.msp.models.User;
import com.msp.payloads.dtos.BranchDto;
import com.msp.payloads.dtos.StoreDto;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.request.CreateBranchRequest;
import com.msp.payloads.request.CreateEmployeeRequest;
import com.msp.payloads.request.CreateStoreRequest;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.StoreRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.BusinessPortalService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessPortalServiceImpl implements BusinessPortalService {

    private final StoreRepository storeRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // ── Store management ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public StoreDto createStore(CreateStoreRequest request) {
        User owner = userService.getCurrentUser();
        UUID tenantId = requireTenantId(owner);

        Store store = new Store();
        store.setBrand(request.getBrand());
        store.setStoreType(request.getStoreType());
        store.setDescription(request.getDescription());
        store.setStoreAdmin(owner);
        store.setTenantId(tenantId);
        applyContact(store, request.getContactPhone(),
                request.getContactEmail(), request.getContactAddress());

        Store saved = storeRepo.save(store);
        log.info("Store created: id={}, brand={}, tenantId={}", saved.getId(), saved.getBrand(), tenantId);
        return StoreMapper.toDto(saved);
    }

    @Override
    public Page<StoreDto> getMyStores(int page, int size) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        return storeRepo.findByTenantId(tenantId, PageRequest.of(page, size))
                .map(StoreMapper::toDto);
    }

    @Override
    @Transactional
    public StoreDto updateStore(UUID storeId, CreateStoreRequest request) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        Store store = findStoreForTenant(storeId, tenantId);

        store.setBrand(request.getBrand());
        if (request.getStoreType()   != null) store.setStoreType(request.getStoreType());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        applyContact(store, request.getContactPhone(),
                request.getContactEmail(), request.getContactAddress());

        return StoreMapper.toDto(storeRepo.save(store));
    }

    @Override
    @Transactional
    public void deleteStore(UUID storeId) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        Store store = findStoreForTenant(storeId, tenantId);
        storeRepo.delete(store);
        log.info("Store deleted: id={}, tenantId={}", storeId, tenantId);
    }

    // ── Branch management ────────────────────────────────────────────────────

    @Override
    @Transactional
    public BranchDto createBranch(CreateBranchRequest request) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());

        // The parent store must belong to this tenant
        Store store = findStoreForTenant(request.getStoreId(), tenantId);

        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setStore(store);
        branch.setTenantId(tenantId);
        branch.setWorkingDays(request.getWorkingDays() != null
                ? request.getWorkingDays() : new ArrayList<>());
        branch.setOpenTime(request.getOpenTime());
        branch.setCloseTime(request.getCloseTime());

        Branch saved = branchRepo.save(branch);
        log.info("Branch created: id={}, name={}, storeId={}, tenantId={}",
                saved.getId(), saved.getName(), store.getId(), tenantId);
        return BranchMapper.toDto(saved);
    }

    @Override
    public Page<BranchDto> getStoreBranches(UUID storeId, int page, int size) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        findStoreForTenant(storeId, tenantId);   // ownership check
        return branchRepo.findByStoreId(storeId, PageRequest.of(page, size))
                .map(BranchMapper::toDto);
    }

    @Override
    @Transactional
    public BranchDto updateBranch(UUID branchId, CreateBranchRequest request) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        Branch branch = findBranchForTenant(branchId, tenantId);

        if (request.getName()    != null) branch.setName(request.getName());
        if (request.getAddress() != null) branch.setAddress(request.getAddress());
        if (request.getPhone()   != null) branch.setPhone(request.getPhone());
        if (request.getEmail()   != null) branch.setEmail(request.getEmail());
        if (request.getWorkingDays() != null) branch.setWorkingDays(request.getWorkingDays());
        if (request.getOpenTime()    != null) branch.setOpenTime(request.getOpenTime());
        if (request.getCloseTime()   != null) branch.setCloseTime(request.getCloseTime());

        // If storeId is changing, validate the new store also belongs to this tenant
        if (request.getStoreId() != null
                && !request.getStoreId().equals(branch.getStore().getId())) {
            Store newStore = findStoreForTenant(request.getStoreId(), tenantId);
            branch.setStore(newStore);
        }

        return BranchMapper.toDto(branchRepo.save(branch));
    }

    @Override
    @Transactional
    public void deleteBranch(UUID branchId) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        Branch branch = findBranchForTenant(branchId, tenantId);
        branchRepo.delete(branch);
        log.info("Branch deleted: id={}, tenantId={}", branchId, tenantId);
    }

    // ── Employee management ──────────────────────────────────────────────────

    @Override
    @Transactional
    public UserDto createEmployee(UUID storeId, CreateEmployeeRequest request) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        Store store = findStoreForTenant(storeId, tenantId);

        // Forbidden roles
        if (request.getRole() == EUserRole.ROLE_STORE_ADMIN
                || request.getRole() == EUserRole.ROLE_SUPER_ADMIN) {
            throw new PortalException("Role " + request.getRole() + " cannot be assigned to employees.");
        }

        // Global email uniqueness
        if (userRepo.findByEmail(request.getEmail()) != null) {
            throw new PortalException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        // Branch required for branch-level roles
        Branch branch = null;
        if (request.getRole() == EUserRole.ROLE_BRANCH_MANAGER
                || request.getRole() == EUserRole.ROLE_BRANCH_CASHIER) {
            if (request.getBranchId() == null) {
                throw new PortalException("branchId is required for role " + request.getRole());
            }
            branch = findBranchForTenant(request.getBranchId(), tenantId);
        }

        User employee = new User();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setRole(request.getRole());
        employee.setUserStatus(EUserStatus.ACTIVE);
        employee.setTenantId(tenantId);
        employee.setStore(store);
        employee.setBranch(branch);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        User saved = userRepo.save(employee);

        // Assign branch manager role on the branch entity
        if (request.getRole() == EUserRole.ROLE_BRANCH_MANAGER && branch != null) {
            branch.setManager(saved);
            branchRepo.save(branch);
        }

        log.info("Employee created: id={}, role={}, tenantId={}",
                saved.getId(), saved.getRole(), tenantId);
        return UserMapper.toDTO(saved);
    }

    @Override
    public Page<UserDto> getStoreEmployees(UUID storeId, int page, int size) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        Store store = findStoreForTenant(storeId, tenantId);
        return userRepo.findByStore(store, PageRequest.of(page, size))
                .map(UserMapper::toDTO);
    }

    @Override
    @Transactional
    public UserDto updateEmployee(UUID employeeId, CreateEmployeeRequest request) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        User employee = findEmployeeForTenant(employeeId, tenantId);

        if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) employee.setLastName(request.getLastName());
        if (request.getPhone()     != null) employee.setPhone(request.getPhone());

        if (request.getRole() != null) {
            if (request.getRole() == EUserRole.ROLE_STORE_ADMIN
                    || request.getRole() == EUserRole.ROLE_SUPER_ADMIN) {
                throw new PortalException(
                        "Role " + request.getRole() + " cannot be assigned to employees.");
            }
            employee.setRole(request.getRole());
        }

        employee.setUpdatedAt(LocalDateTime.now());
        return UserMapper.toDTO(userRepo.save(employee));
    }

    @Override
    @Transactional
    public UserDto dischargeEmployee(UUID employeeId) {
        UUID tenantId = requireTenantId(userService.getCurrentUser());
        User employee = findEmployeeForTenant(employeeId, tenantId);

        if (employee.getUserStatus() == EUserStatus.DISCHARGED) {
            throw new PortalException("Employee is already discharged.");
        }

        employee.setUserStatus(EUserStatus.DISCHARGED);
        employee.setDischargedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        log.info("Employee discharged: id={}, tenantId={}", employeeId, tenantId);
        return UserMapper.toDTO(userRepo.save(employee));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private UUID requireTenantId(User user) {
        if (user.getTenantId() == null) {
            throw new PortalException("Your account is not linked to a business tenant.");
        }
        return user.getTenantId();
    }

    private Store findStoreForTenant(UUID storeId, UUID tenantId) {
        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new PortalException("Store not found: " + storeId));
        if (!tenantId.equals(store.getTenantId())) {
            throw new PortalException("Store does not belong to your business tenant.");
        }
        return store;
    }

    private Branch findBranchForTenant(UUID branchId, UUID tenantId) {
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new PortalException("Branch not found: " + branchId));
        if (!tenantId.equals(branch.getTenantId())) {
            throw new PortalException("Branch does not belong to your business tenant.");
        }
        return branch;
    }

    private User findEmployeeForTenant(UUID employeeId, UUID tenantId) {
        User employee = userRepo.findById(employeeId)
                .orElseThrow(() -> new PortalException("Employee not found: " + employeeId));
        if (!tenantId.equals(employee.getTenantId())) {
            throw new PortalException("Employee does not belong to your business tenant.");
        }
        return employee;
    }

    private void applyContact(Store store, String phone, String email, String address) {
        if (phone == null && email == null && address == null) return;
        StoreContact contact = store.getContact() != null
                ? store.getContact() : new StoreContact();
        if (phone   != null) contact.setPhone(phone);
        if (email   != null) contact.setEmail(email);
        if (address != null) contact.setAddress(address);
        store.setContact(contact);
    }
}
