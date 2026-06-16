package com.msp.configs;

import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.models.User;
import com.msp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Starting Data Initialization ===");

        try {
            String adminEmail = "mahingarodin@gmail.com";
            User adminUser = userRepository.findByEmail(adminEmail);

            if (adminUser == null) {
                log.info("Creating super admin user...");
                adminUser = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin!123"))
                        .firstName("Mahinga")
                        .lastName("Rodin")
                        .role(EUserRole.ROLE_SUPER_ADMIN)
                        .userStatus(EUserStatus.ACTIVE)
                        .phone("+250794415318")
                        .tenantId(null)   // super admin has no tenant
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .lastLogin(LocalDateTime.now())
                        .build();
                userRepository.save(adminUser);
                log.info("Super admin created: {}", adminEmail);
            } else {
                log.info("Super admin already exists: {}", adminEmail);
            }

            log.info("=== Data Initialization Complete! ===");

        } catch (Exception e) {
            log.error("Error during data initialization", e);
            throw e;
        }
    }
}