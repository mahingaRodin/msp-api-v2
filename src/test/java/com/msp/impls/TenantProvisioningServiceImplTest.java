package com.msp.impls;

import com.msp.enums.EBusinessStatus;
import com.msp.enums.ERegistrationStatus;
import com.msp.enums.ESubscriptionTier;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.models.Business;
import com.msp.models.TenantRegistration;
import com.msp.models.User;
import com.msp.payloads.dtos.BusinessDto;
import com.msp.payloads.response.ProvisioningResponse;
import com.msp.repositories.BusinessRepository;
import com.msp.repositories.TenantRegistrationRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantProvisioningServiceImpl Tests")
class TenantProvisioningServiceImplTest {

    @Mock private TenantRegistrationRepository registrationRepo;
    @Mock private BusinessRepository businessRepo;
    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private TenantProvisioningServiceImpl service;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    private TenantRegistration approvedReg(UUID id) {
        TenantRegistration reg = new TenantRegistration();
        reg.setId(id);
        reg.setOwnerFirstName("Alice");
        reg.setOwnerLastName("Doe");
        reg.setOwnerEmail("alice@example.com");
        reg.setOwnerPhone("+250788000000");
        reg.setBusinessName("Alice Retail Ltd");
        reg.setLegalName("Alice Retail Limited");
        reg.setRegistrationNumber("RW-2024-001");
        reg.setCountry("RW");
        reg.setIndustry("RETAIL");
        reg.setBusinessDescription("Multi-branch retail store");
        reg.setStatus(ERegistrationStatus.APPROVED);
        reg.setSubmittedAt(LocalDateTime.now());
        reg.setProvisionedTenantId(null);   // not yet provisioned
        return reg;
    }

    private User savedOwner(UUID tenantId) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFirstName("Alice");
        u.setLastName("Doe");
        u.setEmail("alice@example.com");
        u.setRole(EUserRole.ROLE_STORE_ADMIN);
        u.setUserStatus(EUserStatus.ACTIVE);
        u.setTenantId(tenantId);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    private Business savedBusiness(UUID tenantId, User owner) {
        Business b = new Business();
        b.setId(UUID.randomUUID());
        b.setTenantId(tenantId);
        b.setBusinessName("Alice Retail Ltd");
        b.setOwner(owner);
        b.setSubscriptionTier(ESubscriptionTier.FREE_TRIAL);
        b.setStatus(EBusinessStatus.ACTIVE);
        b.setCreatedAt(LocalDateTime.now());
        b.setTrialEndsAt(LocalDateTime.now().plusDays(30));
        return b;
    }

    // ── provisionTenant ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("provisionTenant()")
    class ProvisionTenant {

        @Test
        @DisplayName("Happy path — creates User + Business, stamps provisionedTenantId, returns response")
        void provision_happyPath_createsUserAndBusiness() {
            UUID regId = UUID.randomUUID();
            TenantRegistration reg = approvedReg(regId);

            when(registrationRepo.findById(regId)).thenReturn(Optional.of(reg));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-temp-password");

            // Capture the User saved so we can use its tenantId for the Business
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepo.save(userCaptor.capture())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            ArgumentCaptor<Business> bizCaptor = ArgumentCaptor.forClass(Business.class);
            when(businessRepo.save(bizCaptor.capture())).thenAnswer(inv -> {
                Business b = inv.getArgument(0);
                b.setId(UUID.randomUUID());
                return b;
            });

            when(registrationRepo.save(any())).thenReturn(reg);

            ProvisioningResponse result = service.provisionTenant(regId);

            // ── Assert response ──
            assertThat(result).isNotNull();
            assertThat(result.getOwnerEmail()).isEqualTo("alice@example.com");
            assertThat(result.getTenantId()).isNotNull();
            assertThat(result.getBusinessId()).isNotNull();
            assertThat(result.getMessage()).contains("provisioned");

            // ── Assert User created correctly ──
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEmail()).isEqualTo("alice@example.com");
            assertThat(capturedUser.getFirstName()).isEqualTo("Alice");
            assertThat(capturedUser.getLastName()).isEqualTo("Doe");
            assertThat(capturedUser.getRole()).isEqualTo(EUserRole.ROLE_STORE_ADMIN);
            assertThat(capturedUser.getUserStatus()).isEqualTo(EUserStatus.ACTIVE);
            assertThat(capturedUser.getTenantId()).isNotNull();
            assertThat(capturedUser.getPassword()).isEqualTo("hashed-temp-password");

            // ── Assert Business created correctly ──
            Business capturedBiz = bizCaptor.getValue();
            assertThat(capturedBiz.getBusinessName()).isEqualTo("Alice Retail Ltd");
            assertThat(capturedBiz.getTenantId()).isEqualTo(capturedUser.getTenantId());
            assertThat(capturedBiz.getOwner()).isEqualTo(capturedUser);

            // ── Assert registration stamped with provisionedTenantId ──
            ArgumentCaptor<TenantRegistration> regCaptor =
                    ArgumentCaptor.forClass(TenantRegistration.class);
            verify(registrationRepo).save(regCaptor.capture());
            assertThat(regCaptor.getValue().getProvisionedTenantId()).isNotNull();
        }

        @Test
        @DisplayName("User tenantId matches Business tenantId")
        void provision_tenantIdConsistency() {
            UUID regId = UUID.randomUUID();
            when(registrationRepo.findById(regId)).thenReturn(Optional.of(approvedReg(regId)));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepo.save(userCaptor.capture())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });

            ArgumentCaptor<Business> bizCaptor = ArgumentCaptor.forClass(Business.class);
            when(businessRepo.save(bizCaptor.capture())).thenAnswer(inv -> {
                Business b = inv.getArgument(0);
                b.setId(UUID.randomUUID());
                return b;
            });

            when(registrationRepo.save(any())).thenReturn(approvedReg(regId));

            service.provisionTenant(regId);

            UUID userTenantId = userCaptor.getValue().getTenantId();
            UUID bizTenantId  = bizCaptor.getValue().getTenantId();

            assertThat(userTenantId).isNotNull();
            assertThat(bizTenantId).isEqualTo(userTenantId);
        }

        @Test
        @DisplayName("Throws when registration is not found")
        void provision_notFound_throws() {
            UUID regId = UUID.randomUUID();
            when(registrationRepo.findById(regId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.provisionTenant(regId))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("not found");

            verify(userRepo, never()).save(any());
            verify(businessRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when registration status is PENDING (not APPROVED)")
        void provision_pendingStatus_throws() {
            UUID regId = UUID.randomUUID();
            TenantRegistration reg = approvedReg(regId);
            reg.setStatus(ERegistrationStatus.PENDING);
            when(registrationRepo.findById(regId)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.provisionTenant(regId))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("not APPROVED");

            verify(userRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when registration status is REJECTED")
        void provision_rejectedStatus_throws() {
            UUID regId = UUID.randomUUID();
            TenantRegistration reg = approvedReg(regId);
            reg.setStatus(ERegistrationStatus.REJECTED);
            when(registrationRepo.findById(regId)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.provisionTenant(regId))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("not APPROVED");
        }

        @Test
        @DisplayName("Throws when tenant already provisioned (idempotency guard)")
        void provision_alreadyProvisioned_throws() {
            UUID regId = UUID.randomUUID();
            TenantRegistration reg = approvedReg(regId);
            reg.setProvisionedTenantId(UUID.randomUUID());   // already set
            when(registrationRepo.findById(regId)).thenReturn(Optional.of(reg));

            assertThatThrownBy(() -> service.provisionTenant(regId))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("already provisioned");

            verify(userRepo, never()).save(any());
            verify(businessRepo, never()).save(any());
        }

        @Test
        @DisplayName("Password is encoded — plain text is never stored")
        void provision_passwordIsEncoded() {
            UUID regId = UUID.randomUUID();
            when(registrationRepo.findById(regId)).thenReturn(Optional.of(approvedReg(regId)));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepo.save(userCaptor.capture())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });
            when(businessRepo.save(any())).thenAnswer(inv -> {
                Business b = inv.getArgument(0);
                b.setId(UUID.randomUUID());
                return b;
            });
            when(registrationRepo.save(any())).thenReturn(approvedReg(regId));

            service.provisionTenant(regId);

            assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
            verify(passwordEncoder).encode(anyString());
        }
    }

    // ── getTenantDetails ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTenantDetails()")
    class GetTenantDetails {

        @Test
        @DisplayName("Returns BusinessDto for existing tenantId")
        void get_exists_returnsDto() {
            UUID tenantId = UUID.randomUUID();
            User owner = savedOwner(tenantId);
            Business business = savedBusiness(tenantId, owner);

            when(businessRepo.findByTenantId(tenantId)).thenReturn(Optional.of(business));

            BusinessDto result = service.getTenantDetails(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.getBusinessName()).isEqualTo("Alice Retail Ltd");
            assertThat(result.getOwnerEmail()).isEqualTo("alice@example.com");
            assertThat(result.getSubscriptionTier()).isEqualTo(ESubscriptionTier.FREE_TRIAL);
        }

        @Test
        @DisplayName("Throws for non-existent tenantId")
        void get_notFound_throws() {
            UUID tenantId = UUID.randomUUID();
            when(businessRepo.findByTenantId(tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTenantDetails(tenantId))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("No business found");
        }
    }
}
