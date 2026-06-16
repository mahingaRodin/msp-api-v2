package com.msp.impls;

import com.msp.enums.ERegistrationStatus;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.models.TenantRegistration;
import com.msp.models.User;
import com.msp.payloads.dtos.TenantRegistrationDto;
import com.msp.payloads.request.BusinessRegistrationRequest;
import com.msp.payloads.response.ProvisioningResponse;
import com.msp.repositories.TenantRegistrationRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.TenantProvisioningService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessRegistrationServiceImpl Tests")
class BusinessRegistrationServiceImplTest {

    @Mock private TenantRegistrationRepository registrationRepo;
    @Mock private UserRepository userRepo;
    @Mock private TenantProvisioningService provisioningService;
    @Mock private com.msp.services.AuditLogService auditLogService;
    @Mock private com.msp.services.AwsSnsService snsService;
    @Mock private com.msp.repositories.OutboxEventRepository outboxRepo;
    @Mock private tools.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private BusinessRegistrationServiceImpl service;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    private BusinessRegistrationRequest validRequest() {
        BusinessRegistrationRequest req = new BusinessRegistrationRequest();
        req.setOwnerFirstName("Alice");
        req.setOwnerLastName("Doe");
        req.setOwnerEmail("alice@example.com");
        req.setOwnerPhone("+250788000000");
        req.setBusinessName("Alice Retail Ltd");
        req.setLegalName("Alice Retail Limited");
        req.setRegistrationNumber("RW-2024-001");
        req.setCountry("RW");
        req.setIndustry("RETAIL");
        req.setBusinessDescription("Multi-branch retail store in Kigali");
        return req;
    }

    private TenantRegistration savedReg(UUID id, ERegistrationStatus status) {
        TenantRegistration reg = new TenantRegistration();
        reg.setId(id);
        reg.setOwnerFirstName("Alice");
        reg.setOwnerLastName("Doe");
        reg.setOwnerEmail("alice@example.com");
        reg.setBusinessName("Alice Retail Ltd");
        reg.setStatus(status);
        reg.setSubmittedAt(LocalDateTime.now());
        return reg;
    }

    // ── submitRegistration ───────────────────────────────────────────────────

    @Nested
    @DisplayName("submitRegistration()")
    class SubmitRegistration {

        @Test
        @DisplayName("Happy path — saves registration with PENDING status and returns DTO")
        void submit_happyPath_savesPendingAndReturnsDto() {
            when(registrationRepo.existsByOwnerEmail("alice@example.com")).thenReturn(false);
            when(userRepo.findByEmail("alice@example.com")).thenReturn(null);
            when(registrationRepo.existsByBusinessName("Alice Retail Ltd")).thenReturn(false);

            UUID savedId = UUID.randomUUID();
            TenantRegistration saved = savedReg(savedId, ERegistrationStatus.PENDING);
            when(registrationRepo.save(any(TenantRegistration.class))).thenReturn(saved);

            TenantRegistrationDto result = service.submitRegistration(validRequest());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedId);
            assertThat(result.getStatus()).isEqualTo(ERegistrationStatus.PENDING);
            assertThat(result.getOwnerEmail()).isEqualTo("alice@example.com");

            // Verify the entity saved has the right fields
            ArgumentCaptor<TenantRegistration> captor = ArgumentCaptor.forClass(TenantRegistration.class);
            verify(registrationRepo).save(captor.capture());
            TenantRegistration captured = captor.getValue();
            assertThat(captured.getOwnerEmail()).isEqualTo("alice@example.com");
            assertThat(captured.getBusinessName()).isEqualTo("Alice Retail Ltd");
            assertThat(captured.getStatus()).isEqualTo(ERegistrationStatus.PENDING);
        }

        @Test
        @DisplayName("Throws when ownerEmail already exists in registrations table")
        void submit_duplicateEmailInRegistrations_throws() {
            when(registrationRepo.existsByOwnerEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.submitRegistration(validRequest()))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("already exists");

            verify(registrationRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when ownerEmail already exists as a User account")
        void submit_emailAlreadyAUser_throws() {
            when(registrationRepo.existsByOwnerEmail("alice@example.com")).thenReturn(false);
            when(userRepo.findByEmail("alice@example.com")).thenReturn(new User());

            assertThatThrownBy(() -> service.submitRegistration(validRequest()))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("already exists");

            verify(registrationRepo, never()).save(any());
        }

        @Test
        @DisplayName("Throws when businessName already exists in registrations table")
        void submit_duplicateBusinessName_throws() {
            when(registrationRepo.existsByOwnerEmail("alice@example.com")).thenReturn(false);
            when(userRepo.findByEmail("alice@example.com")).thenReturn(null);
            when(registrationRepo.existsByBusinessName("Alice Retail Ltd")).thenReturn(true);

            assertThatThrownBy(() -> service.submitRegistration(validRequest()))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("already exists");

            verify(registrationRepo, never()).save(any());
        }
    }

    // ── getRegistration ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRegistration()")
    class GetRegistration {

        @Test
        @DisplayName("Returns DTO for existing registration")
        void get_exists_returnsDto() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.PENDING)));

            TenantRegistrationDto result = service.getRegistration(id);

            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Throws for non-existent registration")
        void get_notFound_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRegistration(id))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── listRegistrations ────────────────────────────────────────────────────

    @Nested
    @DisplayName("listRegistrations()")
    class ListRegistrations {

        @Test
        @DisplayName("Returns all registrations when status is null")
        void list_noFilter_returnsAll() {
            UUID id = UUID.randomUUID();
            Page<TenantRegistration> page = new PageImpl<>(
                    List.of(savedReg(id, ERegistrationStatus.PENDING)));
            when(registrationRepo.findAll(any(PageRequest.class))).thenReturn(page);

            Page<TenantRegistrationDto> result =
                    service.listRegistrations(null, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Filters by status when status is provided")
        void list_withStatusFilter_callsFindByStatus() {
            UUID id = UUID.randomUUID();
            Page<TenantRegistration> page = new PageImpl<>(
                    List.of(savedReg(id, ERegistrationStatus.PENDING)));
            when(registrationRepo.findByStatus(eq(ERegistrationStatus.PENDING), any()))
                    .thenReturn(page);

            Page<TenantRegistrationDto> result =
                    service.listRegistrations(ERegistrationStatus.PENDING, PageRequest.of(0, 10));

            assertThat(result.getContent().get(0).getStatus())
                    .isEqualTo(ERegistrationStatus.PENDING);
            verify(registrationRepo).findByStatus(eq(ERegistrationStatus.PENDING), any());
            verify(registrationRepo, never()).findAll(any(PageRequest.class));
        }
    }

    // ── markUnderReview ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("markUnderReview()")
    class MarkUnderReview {

        @Test
        @DisplayName("PENDING → UNDER_REVIEW succeeds")
        void review_pending_transitionsToUnderReview() {
            UUID id = UUID.randomUUID();
            TenantRegistration reg = savedReg(id, ERegistrationStatus.PENDING);
            when(registrationRepo.findById(id)).thenReturn(Optional.of(reg));

            TenantRegistration updated = savedReg(id, ERegistrationStatus.UNDER_REVIEW);
            when(registrationRepo.save(any())).thenReturn(updated);

            TenantRegistrationDto result = service.markUnderReview(id);

            assertThat(result.getStatus()).isEqualTo(ERegistrationStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("APPROVED → UNDER_REVIEW throws (terminal state)")
        void review_approved_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.APPROVED)));

            assertThatThrownBy(() -> service.markUnderReview(id))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("REJECTED → UNDER_REVIEW throws (terminal state)")
        void review_rejected_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.REJECTED)));

            assertThatThrownBy(() -> service.markUnderReview(id))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("Cannot transition");
        }
    }

    // ── approveRegistration ──────────────────────────────────────────────────

    @Nested
    @DisplayName("approveRegistration()")
    class ApproveRegistration {

        private void mockSecurityContext(String email) {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn(email);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);
        }

        @Test
        @DisplayName("PENDING → APPROVED calls provisionTenant and returns ProvisioningResponse")
        void approve_pending_callsProvisionAndReturnsResponse() {
            UUID id       = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UUID bizId    = UUID.randomUUID();

            TenantRegistration reg = savedReg(id, ERegistrationStatus.PENDING);
            when(registrationRepo.findById(id)).thenReturn(Optional.of(reg));
            when(userRepo.findByEmail(anyString())).thenReturn(null);

            TenantRegistration approved = savedReg(id, ERegistrationStatus.APPROVED);
            when(registrationRepo.save(any())).thenReturn(approved);

            ProvisioningResponse provResponse = new ProvisioningResponse(
                    tenantId, bizId, "alice@example.com", "Tenant provisioned successfully.");
            when(provisioningService.provisionTenant(id)).thenReturn(provResponse);

            mockSecurityContext("admin@system.com");

            ProvisioningResponse result = service.approveRegistration(id, "All good.");

            assertThat(result.getTenantId()).isEqualTo(tenantId);
            assertThat(result.getOwnerEmail()).isEqualTo("alice@example.com");
            verify(provisioningService).provisionTenant(id);

            // Verify the registration was saved with APPROVED status
            ArgumentCaptor<TenantRegistration> captor = ArgumentCaptor.forClass(TenantRegistration.class);
            verify(registrationRepo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ERegistrationStatus.APPROVED);
            assertThat(captor.getValue().getAdminNotes()).isEqualTo("All good.");
        }

        @Test
        @DisplayName("UNDER_REVIEW → APPROVED also succeeds")
        void approve_underReview_succeeds() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.UNDER_REVIEW)));
            when(userRepo.findByEmail(anyString())).thenReturn(null);
            when(registrationRepo.save(any())).thenReturn(savedReg(id, ERegistrationStatus.APPROVED));
            when(provisioningService.provisionTenant(id))
                    .thenReturn(new ProvisioningResponse(UUID.randomUUID(), UUID.randomUUID(),
                            "alice@example.com", "ok"));

            mockSecurityContext("admin@system.com");

            ProvisioningResponse result = service.approveRegistration(id, null);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("APPROVED → APPROVED throws (terminal state)")
        void approve_alreadyApproved_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.APPROVED)));

            assertThatThrownBy(() -> service.approveRegistration(id, null))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("Cannot transition");

            verify(provisioningService, never()).provisionTenant(any());
        }

        @Test
        @DisplayName("REJECTED → APPROVED throws (terminal state)")
        void approve_rejected_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.REJECTED)));

            assertThatThrownBy(() -> service.approveRegistration(id, null))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("Cannot transition");
        }
    }

    // ── rejectRegistration ───────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectRegistration()")
    class RejectRegistration {

        private void mockSecurityContext(String email) {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn(email);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);
        }

        @Test
        @DisplayName("PENDING → REJECTED saves rejection reason and returns REJECTED DTO")
        void reject_pending_savesReasonAndReturnsRejected() {
            UUID id = UUID.randomUUID();
            TenantRegistration reg = savedReg(id, ERegistrationStatus.PENDING);
            when(registrationRepo.findById(id)).thenReturn(Optional.of(reg));
            when(userRepo.findByEmail(anyString())).thenReturn(null);

            TenantRegistration rejected = savedReg(id, ERegistrationStatus.REJECTED);
            rejected.setRejectionReason("Incomplete documents.");
            when(registrationRepo.save(any())).thenReturn(rejected);

            mockSecurityContext("admin@system.com");

            TenantRegistrationDto result = service.rejectRegistration(id, "Incomplete documents.");

            assertThat(result.getStatus()).isEqualTo(ERegistrationStatus.REJECTED);
            assertThat(result.getRejectionReason()).isEqualTo("Incomplete documents.");

            ArgumentCaptor<TenantRegistration> captor = ArgumentCaptor.forClass(TenantRegistration.class);
            verify(registrationRepo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ERegistrationStatus.REJECTED);
            assertThat(captor.getValue().getRejectionReason()).isEqualTo("Incomplete documents.");
        }

        @Test
        @DisplayName("APPROVED → REJECTED throws (terminal state)")
        void reject_approved_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.APPROVED)));

            assertThatThrownBy(() -> service.rejectRegistration(id, "reason"))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("REJECTED → REJECTED throws (terminal state)")
        void reject_alreadyRejected_throws() {
            UUID id = UUID.randomUUID();
            when(registrationRepo.findById(id))
                    .thenReturn(Optional.of(savedReg(id, ERegistrationStatus.REJECTED)));

            assertThatThrownBy(() -> service.rejectRegistration(id, "reason"))
                    .isInstanceOf(BusinessRegistrationException.class)
                    .hasMessageContaining("Cannot transition");
        }
    }
}
