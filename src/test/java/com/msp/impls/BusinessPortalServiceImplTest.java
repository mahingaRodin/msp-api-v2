package com.msp.impls;

import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.PortalException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessPortalServiceImpl Tests")
class BusinessPortalServiceImplTest {

    @Mock private StoreRepository storeRepo;
    @Mock private BranchRepository branchRepo;
    @Mock private UserRepository userRepo;
    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BusinessPortalServiceImpl service;

    private UUID tenantId;
    private User owner;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@business.com");
        owner.setRole(EUserRole.ROLE_STORE_ADMIN);
        owner.setTenantId(tenantId);
        owner.setUserStatus(EUserStatus.ACTIVE);
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Store store(UUID id) {
        Store s = new Store();
        s.setId(id);
        s.setBrand("Alice Retail");
        s.setTenantId(tenantId);
        s.setStoreAdmin(owner);
        s.setContact(new StoreContact());
        return s;
    }

    private Branch branch(UUID id) {
        Branch b = new Branch();
        b.setId(id);
        b.setName("Main Branch");
        b.setTenantId(tenantId);
        return b;
    }

    private Branch branchWithStore(UUID id, Store s) {
        Branch b = branch(id);
        b.setStore(s);
        return b;
    }

    private CreateStoreRequest storeRequest(String brand) {
        CreateStoreRequest req = new CreateStoreRequest();
        req.setBrand(brand);
        req.setStoreType("RETAIL");
        req.setDescription("Test store");
        return req;
    }

    private CreateBranchRequest branchRequest(UUID storeId) {
        CreateBranchRequest req = new CreateBranchRequest();
        req.setName("Main Branch");
        req.setAddress("Kigali");
        req.setPhone("+250788000001");
        req.setStoreId(storeId);
        return req;
    }

    private CreateEmployeeRequest employeeRequest(EUserRole role, UUID branchId) {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFirstName("Bob");
        req.setLastName("Smith");
        req.setEmail("bob@business.com");
        req.setPhone("+250788000002");
        req.setPassword("pass123");
        req.setRole(role);
        req.setBranchId(branchId);
        return req;
    }

    // ── createStore() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createStore()")
    class CreateStore {

        @Test
        @DisplayName("Happy path — saves store with correct tenantId and owner")
        void create_happyPath_savesCorrectly() {
            when(userService.getCurrentUser()).thenReturn(owner);

            ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
            when(storeRepo.save(captor.capture())).thenAnswer(inv -> {
                Store s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });

            StoreDto result = service.createStore(storeRequest("Alice Retail"));

            assertThat(result).isNotNull();
            Store saved = captor.getValue();
            assertThat(saved.getBrand()).isEqualTo("Alice Retail");
            assertThat(saved.getTenantId()).isEqualTo(tenantId);
            assertThat(saved.getStoreAdmin()).isEqualTo(owner);
        }

        @Test
        @DisplayName("Throws when owner has no tenantId")
        void create_noTenantId_throws() {
            owner.setTenantId(null);
            when(userService.getCurrentUser()).thenReturn(owner);

            assertThatThrownBy(() -> service.createStore(storeRequest("X")))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not linked to a business tenant");

            verify(storeRepo, never()).save(any());
        }
    }

    // ── getMyStores() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyStores()")
    class GetMyStores {

        @Test
        @DisplayName("Returns paginated stores for the tenant")
        void get_returnsPage() {
            UUID storeId = UUID.randomUUID();
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findByTenantId(eq(tenantId), any()))
                    .thenReturn(new PageImpl<>(List.of(store(storeId))));

            Page<StoreDto> result = service.getMyStores(0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTenantId()).isEqualTo(tenantId);
        }
    }

    // ── updateStore() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStore()")
    class UpdateStore {

        @Test
        @DisplayName("Updates brand on a store owned by the tenant")
        void update_ownedStore_succeeds() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(storeRepo.save(any())).thenReturn(s);

            CreateStoreRequest req = storeRequest("Updated Brand");
            service.updateStore(storeId, req);

            verify(storeRepo).save(argThat(saved -> saved.getBrand().equals("Updated Brand")));
        }

        @Test
        @DisplayName("Throws when store belongs to a different tenant")
        void update_foreignStore_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            s.setTenantId(UUID.randomUUID());   // different tenant
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.updateStore(storeId, storeRequest("X")))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Throws when store not found")
        void update_notFound_throws() {
            UUID storeId = UUID.randomUUID();
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStore(storeId, storeRequest("X")))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── deleteStore() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteStore()")
    class DeleteStore {

        @Test
        @DisplayName("Deletes a store owned by the tenant")
        void delete_ownedStore_succeeds() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            service.deleteStore(storeId);

            verify(storeRepo).delete(s);
        }

        @Test
        @DisplayName("Throws when store belongs to a different tenant")
        void delete_foreignStore_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            s.setTenantId(UUID.randomUUID());
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.deleteStore(storeId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");

            verify(storeRepo, never()).delete(any());
        }
    }

    // ── createBranch() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBranch()")
    class CreateBranch {

        @Test
        @DisplayName("Happy path — saves branch with correct tenantId and store link")
        void create_happyPath_savesCorrectly() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            when(branchRepo.save(captor.capture())).thenAnswer(inv -> {
                Branch b = inv.getArgument(0);
                b.setId(UUID.randomUUID());
                return b;
            });

            BranchDto result = service.createBranch(branchRequest(storeId));

            assertThat(result).isNotNull();
            Branch saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Main Branch");
            assertThat(saved.getTenantId()).isEqualTo(tenantId);
            assertThat(saved.getStore()).isEqualTo(s);
        }

        @Test
        @DisplayName("Throws when parent store belongs to a different tenant")
        void create_foreignStore_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            s.setTenantId(UUID.randomUUID());   // different tenant
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.createBranch(branchRequest(storeId)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");

            verify(branchRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when store not found")
        void create_storeNotFound_throws() {
            UUID storeId = UUID.randomUUID();
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createBranch(branchRequest(storeId)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── updateBranch() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateBranch()")
    class UpdateBranch {

        @Test
        @DisplayName("Updates branch name for a branch owned by the tenant")
        void update_ownedBranch_succeeds() {
            UUID storeId  = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            Store s  = store(storeId);
            Branch b = branchWithStore(branchId, s);

            when(userService.getCurrentUser()).thenReturn(owner);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));
            when(branchRepo.save(any())).thenReturn(b);

            CreateBranchRequest req = branchRequest(storeId);
            req.setName("Updated Branch");
            service.updateBranch(branchId, req);

            verify(branchRepo).save(argThat(saved -> saved.getName().equals("Updated Branch")));
        }

        @Test
        @DisplayName("Throws when branch belongs to a different tenant")
        void update_foreignBranch_throws() {
            UUID branchId = UUID.randomUUID();
            Branch b = branch(branchId);
            b.setTenantId(UUID.randomUUID());   // different tenant
            when(userService.getCurrentUser()).thenReturn(owner);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.updateBranch(branchId, branchRequest(UUID.randomUUID())))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ── deleteBranch() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteBranch()")
    class DeleteBranch {

        @Test
        @DisplayName("Deletes a branch owned by the tenant")
        void delete_ownedBranch_succeeds() {
            UUID branchId = UUID.randomUUID();
            Branch b = branch(branchId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));

            service.deleteBranch(branchId);

            verify(branchRepo).delete(b);
        }

        @Test
        @DisplayName("Throws when branch belongs to a different tenant")
        void delete_foreignBranch_throws() {
            UUID branchId = UUID.randomUUID();
            Branch b = branch(branchId);
            b.setTenantId(UUID.randomUUID());
            when(userService.getCurrentUser()).thenReturn(owner);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.deleteBranch(branchId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");

            verify(branchRepo, never()).delete(any());
        }
    }

    // ── createEmployee() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("createEmployee()")
    class CreateEmployee {

        @Test
        @DisplayName("Creates STORE_MANAGER with correct tenantId, store, and hashed password")
        void create_storeManager_savesCorrectly() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(userRepo.findByEmail("bob@business.com")).thenReturn(null);
            when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepo.save(captor.capture())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            UserDto result = service.createEmployee(storeId,
                    employeeRequest(EUserRole.ROLE_STORE_MANAGER, null));

            assertThat(result).isNotNull();
            User saved = captor.getValue();
            assertThat(saved.getRole()).isEqualTo(EUserRole.ROLE_STORE_MANAGER);
            assertThat(saved.getTenantId()).isEqualTo(tenantId);
            assertThat(saved.getStore()).isEqualTo(s);
            assertThat(saved.getPassword()).isEqualTo("$2a$hashed");
            assertThat(saved.getUserStatus()).isEqualTo(EUserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Creates BRANCH_CASHIER and links to branch")
        void create_branchCashier_linksToCorrectBranch() {
            UUID storeId  = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            Store s  = store(storeId);
            Branch b = branch(branchId);

            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(userRepo.findByEmail("bob@business.com")).thenReturn(null);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepo.save(captor.capture())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            service.createEmployee(storeId,
                    employeeRequest(EUserRole.ROLE_BRANCH_CASHIER, branchId));

            assertThat(captor.getValue().getBranch()).isEqualTo(b);
        }

        @Test
        @DisplayName("Creates BRANCH_MANAGER and sets them as branch.manager")
        void create_branchManager_setsBranchManager() {
            UUID storeId  = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            Store s  = store(storeId);
            Branch b = branch(branchId);

            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(userRepo.findByEmail("bob@business.com")).thenReturn(null);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(userRepo.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            service.createEmployee(storeId,
                    employeeRequest(EUserRole.ROLE_BRANCH_MANAGER, branchId));

            // Branch should be saved with the manager set
            verify(branchRepo).save(argThat(saved -> saved.getManager() != null));
        }

        @Test
        @DisplayName("Throws when role is ROLE_STORE_ADMIN")
        void create_forbiddenRole_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.createEmployee(storeId,
                    employeeRequest(EUserRole.ROLE_STORE_ADMIN, null)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("cannot be assigned");

            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when email already exists globally")
        void create_duplicateEmail_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(userRepo.findByEmail("bob@business.com")).thenReturn(new User());

            assertThatThrownBy(() -> service.createEmployee(storeId,
                    employeeRequest(EUserRole.ROLE_STORE_MANAGER, null)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("already exists");

            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when branchId is missing for BRANCH_CASHIER")
        void create_missingBranchId_throws() {
            UUID storeId = UUID.randomUUID();
            Store s = store(storeId);
            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(userRepo.findByEmail("bob@business.com")).thenReturn(null);

            CreateEmployeeRequest req = employeeRequest(EUserRole.ROLE_BRANCH_CASHIER, null);
            req.setBranchId(null);   // missing

            assertThatThrownBy(() -> service.createEmployee(storeId, req))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("branchId is required");
        }

        @Test
        @DisplayName("Throws when branch belongs to a different tenant")
        void create_foreignBranch_throws() {
            UUID storeId  = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            Store s  = store(storeId);
            Branch b = branch(branchId);
            b.setTenantId(UUID.randomUUID());   // different tenant

            when(userService.getCurrentUser()).thenReturn(owner);
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(userRepo.findByEmail("bob@business.com")).thenReturn(null);
            when(branchRepo.findById(branchId)).thenReturn(Optional.of(b));

            assertThatThrownBy(() -> service.createEmployee(storeId,
                    employeeRequest(EUserRole.ROLE_BRANCH_CASHIER, branchId)))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ── dischargeEmployee() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("dischargeEmployee()")
    class DischargeEmployee {

        @Test
        @DisplayName("Sets status to DISCHARGED and stamps dischargedAt")
        void discharge_active_succeeds() {
            UUID empId = UUID.randomUUID();
            User emp = new User();
            emp.setId(empId);
            emp.setTenantId(tenantId);
            emp.setUserStatus(EUserStatus.ACTIVE);

            when(userService.getCurrentUser()).thenReturn(owner);
            when(userRepo.findById(empId)).thenReturn(Optional.of(emp));
            when(userRepo.save(any())).thenReturn(emp);

            UserDto result = service.dischargeEmployee(empId);

            assertThat(result).isNotNull();
            verify(userRepo).save(argThat(saved ->
                    saved.getUserStatus() == EUserStatus.DISCHARGED
                    && saved.getDischargedAt() != null));
        }

        @Test
        @DisplayName("Throws when employee is already discharged")
        void discharge_alreadyDischarged_throws() {
            UUID empId = UUID.randomUUID();
            User emp = new User();
            emp.setId(empId);
            emp.setTenantId(tenantId);
            emp.setUserStatus(EUserStatus.DISCHARGED);

            when(userService.getCurrentUser()).thenReturn(owner);
            when(userRepo.findById(empId)).thenReturn(Optional.of(emp));

            assertThatThrownBy(() -> service.dischargeEmployee(empId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("already discharged");
        }

        @Test
        @DisplayName("Throws when employee belongs to a different tenant")
        void discharge_foreignEmployee_throws() {
            UUID empId = UUID.randomUUID();
            User emp = new User();
            emp.setId(empId);
            emp.setTenantId(UUID.randomUUID());   // different tenant

            when(userService.getCurrentUser()).thenReturn(owner);
            when(userRepo.findById(empId)).thenReturn(Optional.of(emp));

            assertThatThrownBy(() -> service.dischargeEmployee(empId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("does not belong");
        }

        @Test
        @DisplayName("Throws when employee not found")
        void discharge_notFound_throws() {
            UUID empId = UUID.randomUUID();
            when(userService.getCurrentUser()).thenReturn(owner);
            when(userRepo.findById(empId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.dischargeEmployee(empId))
                    .isInstanceOf(PortalException.class)
                    .hasMessageContaining("not found");
        }
    }
}
