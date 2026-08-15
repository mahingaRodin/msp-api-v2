package com.msp.impls;

import com.msp.configs.JwtProvider;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.UserException;
import com.msp.mappers.UserMapper;
import com.msp.models.User;
import com.msp.payloads.dtos.UpdateUserDto;
import com.msp.payloads.dtos.UserDto;
import com.msp.repositories.UserRepository;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "users")
public class UserServiceImpl implements UserService {
    @Autowired
    private final UserRepository userRepo;
    private final JwtProvider provider;
    private final PasswordEncoder passwordEncoder;

    @Override
    @CacheEvict(value = "users-page", allEntries = true)
    public User createUser(UserDto dto) throws UserException {
        User existing = userRepo.findByEmail(dto.getEmail());
        if (existing != null) {
            throw new UserException("Email already exists!");
        }

        User user = UserMapper.toEntity(dto);
        user.setId(null);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setUserStatus(EUserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepo.save(user);
    }

    @Override
    public User getCurrentUserFromToken(String token) throws UserException {
        String email = provider.getEmailFromToken(token);
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserException("Invalid Token!");
        }
        // Check status on login
        if (user.getUserStatus() != EUserStatus.ACTIVE) {
            throw new UserException("Account is " + user.getUserStatus().toString().toLowerCase());
        }
        return user;
    }

    @Override
    public User getCurrentUser() throws UserException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUserByEmail(email);
    }

    @Override
    public User getUserByEmail(String email) throws UserException {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserException("User Not Found!");
        }
        return user;
    }

    @Override
    @Cacheable(key = "#id")
    public User getUserById(UUID id) throws UserException {
        return userRepo.findById(id).orElse(null);
    }

    @Override
    @Cacheable(value = "users-page", key = "#page + '-' + #size + '-' + #sortBy + '-' + #status")
    public Page<UserDto> getAllUsers(int page, int size, String sortBy, EUserStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        if (status != null) {
            return userRepo.findByUserStatus(status, pageable).map(UserMapper::toDTO);
        }
        return userRepo.findAll(pageable).map(UserMapper::toDTO);
    }

    @Override
    @CachePut(key = "#id")
    @CacheEvict(value = {"users-by-token", "users-page"}, allEntries = true)
    public User updateUser(UUID id, UpdateUserDto dto) throws UserException {
        User existing = userRepo.findById(id)
                .orElseThrow(() -> new UserException("User not found!"));

        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) existing.setLastName(dto.getLastName());
        if (dto.getEmail() != null && !dto.getEmail().equals(existing.getEmail())) {
            User check = userRepo.findByEmail(dto.getEmail());
            if (check != null) {
                throw new UserException("Email already in use!");
            }
            existing.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
        if (dto.getRole() != null) existing.setRole(EUserRole.valueOf(dto.getRole()));

        existing.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(existing);
    }

    @Override
    @Transactional
    @CachePut(key = "#id")
    @CacheEvict(value = {"users-by-token", "users-page"}, allEntries = true)
    public User dischargeUser(UUID id, String reason) throws UserException {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserException("User not found!"));

        if (user.getUserStatus() == EUserStatus.DISCHARGED) {
            throw new UserException("User is already discharged!");
        }

        user.setUserStatus(EUserStatus.DISCHARGED);
        user.setDischargedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }

    @Override
    @Transactional
    @CachePut(key = "#id")
    @CacheEvict(value = {"users-by-token", "users-page"}, allEntries = true)
    public User suspendUser(UUID id, String reason) throws UserException {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserException("User not found!"));

        if (user.getUserStatus() == EUserStatus.DISCHARGED) {
            throw new UserException("Cannot suspend a discharged user!");
        }
        if (user.getUserStatus() == EUserStatus.SUSPENDED) {
            throw new UserException("User is already suspended!");
        }

        user.setUserStatus(EUserStatus.SUSPENDED);
        user.setSuspendedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }

    @Override
    @Transactional
    @CachePut(key = "#id")
    @CacheEvict(value = {"users-by-token", "users-page"}, allEntries = true)
    public User activateUser(UUID id) throws UserException {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserException("User not found!"));

        if (user.getUserStatus() == EUserStatus.DISCHARGED) {
            throw new UserException("Cannot reactivate a discharged user!");
        }
        if (user.getUserStatus() == EUserStatus.ACTIVE) {
            throw new UserException("User is already active!");
        }

        user.setUserStatus(EUserStatus.ACTIVE);
        user.setSuspendedAt(null);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }

    @Override
    public boolean canDeleteUser(UUID id) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return false;
        return user.getStore() == null && user.getBranch() == null;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "users-by-token", "users-page"}, allEntries = true)
    public void deleteUser(UUID id) throws UserException {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new UserException("User not found!"));

        if (user.getStore() != null || user.getBranch() != null) {
            throw new UserException("Cannot delete user assigned to store or branch. Use discharge instead.");
        }

        userRepo.delete(user);
    }
}