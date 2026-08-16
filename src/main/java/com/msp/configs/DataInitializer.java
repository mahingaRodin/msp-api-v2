package com.msp.configs;

import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.models.User;
import com.msp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ensures the platform super admin can always sign in.
 * No activation email is required for this account — it is verified on every boot.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:mahingarodin@gmail.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin!123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Starting Data Initialization ===");

        try {
            ensureSuperAdmin();
            log.info("=== Data Initialization Complete! ===");
        } catch (Exception e) {
            log.error("Error during data initialization — continuing without blocking startup", e);
        }
    }

    private void ensureSuperAdmin() {
        List<User> matches = userRepository.findAllByEmail(adminEmail);
        User adminUser = matches.stream()
                .filter(u -> u.getRole() == EUserRole.ROLE_SUPER_ADMIN)
                .findFirst()
                .orElse(matches.isEmpty() ? null : matches.get(0));

        if (adminUser == null) {
            log.info("Creating super admin user...");
            adminUser = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName("Mahinga")
                    .lastName("Rodin")
                    .role(EUserRole.ROLE_SUPER_ADMIN)
                    .userStatus(EUserStatus.ACTIVE)
                    .emailVerified(true)
                    .phone("+250794415318")
                    .tenantId(null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .lastLogin(LocalDateTime.now())
                    .build();
            userRepository.save(adminUser);
            log.info("Super admin created and verified: {}", adminEmail);
            return;
        }

        boolean changed = false;
        if (adminUser.getRole() != EUserRole.ROLE_SUPER_ADMIN) {
            adminUser.setRole(EUserRole.ROLE_SUPER_ADMIN);
            changed = true;
        }
        if (adminUser.getUserStatus() != EUserStatus.ACTIVE) {
            adminUser.setUserStatus(EUserStatus.ACTIVE);
            changed = true;
        }
        if (!adminUser.isEmailVerified()) {
            adminUser.setEmailVerified(true);
            changed = true;
        }
        if (adminUser.getTenantId() != null) {
            adminUser.setTenantId(null);
            changed = true;
        }
        // Clear leftover activation / OTP state that can block login
        if (adminUser.getOtpHash() != null || adminUser.getOtpExpiresAt() != null) {
            adminUser.setOtpHash(null);
            adminUser.setOtpExpiresAt(null);
            changed = true;
        }

        if (changed) {
            adminUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(adminUser);
            log.info("Super admin repaired to ACTIVE + verified: {}", adminEmail);
        } else {
            log.info("Super admin already active: {}", adminEmail);
        }

        // Prefer one ACTIVE super-admin when duplicates exist for the same email
        if (matches.size() > 1) {
            for (User dup : matches) {
                if (dup.getId().equals(adminUser.getId())) continue;
                if (dup.getRole() == EUserRole.ROLE_SUPER_ADMIN
                        && dup.getUserStatus() == EUserStatus.PENDING) {
                    dup.setUserStatus(EUserStatus.DISCHARGED);
                    dup.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(dup);
                    log.warn("Discharged duplicate PENDING super-admin row id={} for {}", dup.getId(), adminEmail);
                }
            }
        }
    }
}
