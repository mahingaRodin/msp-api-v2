package com.msp.impls;

import com.msp.enums.EActivationPurpose;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.mappers.UserMapper;
import com.msp.models.Branch;
import com.msp.models.Store;
import com.msp.models.User;
import com.msp.payloads.dtos.UserDto;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.StoreRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AccountActivationService;
import com.msp.services.EmployeeService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "employees")
public class EmployeeServiceImpl implements EmployeeService {
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationService activationService;
    private final UserService userService;

    private static final Set<EUserRole> STORE_STAFF_ROLES = EnumSet.of(
            EUserRole.ROLE_STORE_MANAGER,
            EUserRole.ROLE_BRANCH_MANAGER,
            EUserRole.ROLE_BRANCH_CASHIER
    );

    private void assertCanManageStore(UUID storeId) {
        User current = userService.getCurrentUser();
        if (current.getRole() == EUserRole.ROLE_SUPER_ADMIN) {
            return;
        }
        if (current.getStore() == null || current.getStore().getId() == null
                || !current.getStore().getId().equals(storeId)) {
            throw new UserException("You can only manage employees for your own store");
        }
    }

    private void assertCanManageBranch(UUID branchId) {
        User current = userService.getCurrentUser();
        if (current.getRole() == EUserRole.ROLE_SUPER_ADMIN) {
            return;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new UserException("Branch Not Found!"));
        if (current.getRole() == EUserRole.ROLE_BRANCH_MANAGER) {
            if (current.getBranch() == null || !branchId.equals(current.getBranch().getId())) {
                throw new UserException("You can only manage employees for your own branch");
            }
            return;
        }
        if (current.getStore() == null || branch.getStore() == null
                || !current.getStore().getId().equals(branch.getStore().getId())) {
            throw new UserException("You can only manage employees for your own store");
        }
    }

    @Override
    @Caching(
            put = {
                    @CachePut(key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "employees-store", allEntries = true),
                    @CacheEvict(value = "employees-branch", allEntries = true)
            }
    )
    public UserDto createStoreEmployee(UserDto employee, UUID storeId) throws Exception {
        assertCanManageStore(storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new Exception("Store Not Found")
        );
        Branch branch = null;
        if(employee.getRole() == EUserRole.ROLE_BRANCH_MANAGER) {
            if(employee.getBranchId() == null) {
                throw new Exception("Branch Id is required to create Branch manager!");
            }
            branch = branchRepository.findById(employee.getBranchId()).orElseThrow(
                    () -> new Exception("Branch Not Found")
            );}
        User user = UserMapper.toEntity(employee);
        user.setStore(store);
        user.setBranch(branch);
        user.setTenantId(store.getTenantId());
        String raw = employee.getPassword() != null && !employee.getPassword().isBlank()
                ? employee.getPassword() : java.util.UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(raw));
        user.setUserStatus(EUserStatus.PENDING);
        user.setEmailVerified(true);
        user.setRole(employee.getRole() != null ? employee.getRole() : EUserRole.ROLE_STORE_MANAGER);

        User savedEmployee = userRepository.save(user);
        if(employee.getRole()==EUserRole.ROLE_BRANCH_MANAGER && branch!=null) {
            branch.setManager(savedEmployee);
            branchRepository.save(branch);
        }
        activationService.sendActivation(savedEmployee,
                employee.getRole() == EUserRole.ROLE_BRANCH_MANAGER
                        ? EActivationPurpose.BRANCH_MANAGER
                        : EActivationPurpose.STORE_MANAGER);
        return UserMapper.toDTO(savedEmployee);
    }

    @Override
    @Caching(
            put = {
                    @CachePut(key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "employees-store", allEntries = true),
                    @CacheEvict(value = "employees-branch", allEntries = true)
            }
    )
    public UserDto createBranchEmployee(UserDto employee, UUID branchId) throws Exception {
        assertCanManageBranch(branchId);
        Branch branch = branchRepository.findById(branchId).orElseThrow(
                () -> new Exception("Branch Not Found!")
        );
        if(employee.getRole() == EUserRole.ROLE_BRANCH_CASHIER
                || employee.getRole() == EUserRole.ROLE_BRANCH_MANAGER) {
            User user = UserMapper.toEntity(employee);
            user.setBranch(branch);
            user.setStore(branch.getStore());
            user.setTenantId(branch.getTenantId() != null
                    ? branch.getTenantId()
                    : (branch.getStore() != null ? branch.getStore().getTenantId() : null));
            String raw = employee.getPassword() != null && !employee.getPassword().isBlank()
                    ? employee.getPassword() : java.util.UUID.randomUUID().toString();
            user.setPassword(passwordEncoder.encode(raw));
            user.setUserStatus(EUserStatus.PENDING);
            user.setEmailVerified(true);
            user.setRole(employee.getRole());
            User saved = userRepository.save(user);
            activationService.sendActivation(saved,
                    employee.getRole() == EUserRole.ROLE_BRANCH_CASHIER
                            ? EActivationPurpose.CASHIER
                            : EActivationPurpose.BRANCH_MANAGER);
            return UserMapper.toDTO(saved);
        }
        throw new Exception("Branch Role Not Supported!");
    }

    @Override
    @Caching(
            put = {
                    @CachePut(key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "employees-store", allEntries = true),
                    @CacheEvict(value = "employees-branch", allEntries = true)
            }
    )
    public User updateEmployee(UUID employeeId, UserDto employeeDetails) throws Exception {
        User existingEmployee = userRepository.findById(employeeId).orElseThrow(
                () -> new Exception("Employee with given id doesn't exist!")
        );
        if (existingEmployee.getStore() != null) {
            assertCanManageStore(existingEmployee.getStore().getId());
        } else if (existingEmployee.getBranch() != null) {
            assertCanManageBranch(existingEmployee.getBranch().getId());
        }
        if (employeeDetails.getBranchId() != null) {
            Branch branch = branchRepository.findById(employeeDetails.getBranchId()).orElseThrow(
                    () -> new Exception("Branch Not Found!")
            );
            existingEmployee.setBranch(branch);
        }
        existingEmployee.setEmail(employeeDetails.getEmail());
        existingEmployee.setFirstName(employeeDetails.getFirstName());
        existingEmployee.setLastName(employeeDetails.getLastName());
        if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isBlank()) {
            existingEmployee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        }
        if (employeeDetails.getRole() != null) {
            existingEmployee.setRole(employeeDetails.getRole());
        }
        return userRepository.save(existingEmployee);
    }

    @Override
    @Caching(
            evict = {
                    @CacheEvict(key = "#employeeId"),
                    @CacheEvict(value = "employees-store", allEntries = true),
                    @CacheEvict(value = "employees-branch", allEntries = true)
            }
    )
    public void deleteEmployee(UUID employeeId) throws Exception {
        User employee = userRepository.findById(employeeId).orElseThrow(
                () -> new Exception("Employee Not Found!")
        );
        if (employee.getStore() != null) {
            assertCanManageStore(employee.getStore().getId());
        } else if (employee.getBranch() != null) {
            assertCanManageBranch(employee.getBranch().getId());
        }
        if (employee.getRole() == EUserRole.ROLE_SUPER_ADMIN
                || employee.getRole() == EUserRole.ROLE_STORE_ADMIN) {
            throw new UserException("Cannot remove admin accounts from the employee list");
        }
        userRepository.delete(employee);
    }

    @Override
    public Page<UserDto> findStoreEmployees(UUID storeId, EUserRole role, int page, int size) throws Exception {
        assertCanManageStore(storeId);
        Store store = storeRepository.findById(storeId).orElseThrow(
                ()-> new Exception("Store Not Found!")
        );
        User current = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        List<UserDto> all = userRepository.findByStore(store, PageRequest.of(0, 500)).stream()
                .filter(user -> user.getRole() != EUserRole.ROLE_SUPER_ADMIN)
                .filter(user -> user.getRole() != EUserRole.ROLE_CUSTOMER)
                .filter(user -> current.getRole() != EUserRole.ROLE_STORE_MANAGER
                        || STORE_STAFF_ROLES.contains(user.getRole()))
                .filter(user -> role == null || user.getRole() == role)
                .map(UserMapper::toDTO)
                .toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }

    @Override
    public Page<UserDto> findBranchEmployees(UUID branchId, EUserRole role, int page, int size) throws Exception {
        assertCanManageBranch(branchId);
        branchRepository.findById(branchId).orElseThrow(
                () -> new Exception("Branch Not Found!")
        );
        Pageable pageable = PageRequest.of(page, size);
        List<UserDto> all = userRepository.findByBranchId(branchId, PageRequest.of(0, 500)).stream()
                .filter(user -> user.getRole() != EUserRole.ROLE_SUPER_ADMIN)
                .filter(user -> user.getRole() != EUserRole.ROLE_STORE_ADMIN)
                .filter(user -> role == null || user.getRole() == role)
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }
}
