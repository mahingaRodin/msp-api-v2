package com.msp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.enums.ERegistrationStatus;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.payloads.dtos.TenantRegistrationDto;
import com.msp.payloads.request.BusinessRegistrationRequest;
import com.msp.payloads.request.RegistrationDecisionRequest;
import com.msp.payloads.response.ProvisioningResponse;
import com.msp.payloads.response.RegistrationSubmittedResponse;
import com.msp.services.AwsS3Service;
import com.msp.services.BusinessRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusinessRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BusinessRegistrationController Tests")
class BusinessRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private BusinessRegistrationService registrationService;

    @MockitoBean
    private AwsS3Service s3Service;

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

    private TenantRegistrationDto pendingDto(UUID id) {
        TenantRegistrationDto dto = new TenantRegistrationDto();
        dto.setId(id);
        dto.setOwnerEmail("alice@example.com");
        dto.setBusinessName("Alice Retail Ltd");
        dto.setStatus(ERegistrationStatus.PENDING);
        dto.setSubmittedAt(LocalDateTime.now());
        return dto;
    }

    // ── POST /api/registrations ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/registrations — Submit Registration")
    class SubmitRegistration {

        @Test
        @DisplayName("Valid request returns 202 Accepted with registrationId and PENDING status")
        void submit_validRequest_returns202() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.submitRegistration(any(BusinessRegistrationRequest.class)))
                    .thenReturn(pendingDto(regId));

            mockMvc.perform(post("/api/registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.registrationId").value(regId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("Missing required fields returns 400 Bad Request")
        void submit_missingEmail_returns400() throws Exception {
            BusinessRegistrationRequest req = validRequest();
            req.setOwnerEmail(null);   // violates @NotBlank

            mockMvc.perform(post("/api/registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Invalid email format returns 400 Bad Request")
        void submit_invalidEmail_returns400() throws Exception {
            BusinessRegistrationRequest req = validRequest();
            req.setOwnerEmail("not-an-email");

            mockMvc.perform(post("/api/registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Duplicate email returns 409 Conflict")
        void submit_duplicateEmail_returns409() throws Exception {
            Mockito.when(registrationService.submitRegistration(any()))
                    .thenThrow(new BusinessRegistrationException(
                            "A registration with email 'alice@example.com' already exists."));

            mockMvc.perform(post("/api/registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            "A registration with email 'alice@example.com' already exists."));
        }

        @Test
        @DisplayName("Duplicate business name returns 409 Conflict")
        void submit_duplicateBusinessName_returns409() throws Exception {
            Mockito.when(registrationService.submitRegistration(any()))
                    .thenThrow(new BusinessRegistrationException(
                            "A registration with business name 'Alice Retail Ltd' already exists."));

            mockMvc.perform(post("/api/registrations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // ── GET /api/registrations/{id}/status ──────────────────────────────────

    @Nested
    @DisplayName("GET /api/registrations/{id}/status — Check Status")
    class GetStatus {

        @Test
        @DisplayName("Existing registration returns 200 with status")
        void getStatus_exists_returns200() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.getRegistration(regId))
                    .thenReturn(pendingDto(regId));

            mockMvc.perform(get("/api/registrations/{id}/status", regId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(regId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Non-existent registration returns 404")
        void getStatus_notFound_returns404() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.getRegistration(regId))
                    .thenThrow(new BusinessRegistrationException("Registration not found: " + regId));

            mockMvc.perform(get("/api/registrations/{id}/status", regId))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/admin/registrations ────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/admin/registrations — List Registrations (Admin)")
    class ListRegistrations {

        @Test
        @DisplayName("Returns paginated list of all registrations")
        void list_noFilter_returnsPage() throws Exception {
            UUID regId = UUID.randomUUID();
            Page<TenantRegistrationDto> page = new PageImpl<>(List.of(pendingDto(regId)));
            Mockito.when(registrationService.listRegistrations(isNull(), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/admin/registrations")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(regId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Filters by PENDING status")
        void list_filterByPending_returnsFilteredPage() throws Exception {
            UUID regId = UUID.randomUUID();
            Page<TenantRegistrationDto> page = new PageImpl<>(List.of(pendingDto(regId)));
            Mockito.when(registrationService.listRegistrations(eq(ERegistrationStatus.PENDING), any()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/admin/registrations")
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("Empty list returns 200 with empty content array")
        void list_empty_returns200WithEmptyContent() throws Exception {
            Mockito.when(registrationService.listRegistrations(any(), any()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/admin/registrations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── POST /api/admin/registrations/{id}/review ────────────────────────────

    @Nested
    @DisplayName("POST /api/admin/registrations/{id}/review — Mark Under Review")
    class MarkUnderReview {

        @Test
        @DisplayName("PENDING registration transitions to UNDER_REVIEW")
        void review_pending_returnsUnderReview() throws Exception {
            UUID regId = UUID.randomUUID();
            TenantRegistrationDto dto = pendingDto(regId);
            dto.setStatus(ERegistrationStatus.UNDER_REVIEW);
            Mockito.when(registrationService.markUnderReview(regId)).thenReturn(dto);

            mockMvc.perform(post("/api/admin/registrations/{id}/review", regId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        }

        @Test
        @DisplayName("Transitioning an APPROVED registration returns 422")
        void review_alreadyApproved_returns422() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.markUnderReview(regId))
                    .thenThrow(new BusinessRegistrationException(
                            "Cannot transition registration from APPROVED to UNDER_REVIEW"));

            mockMvc.perform(post("/api/admin/registrations/{id}/review", regId))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ── POST /api/admin/registrations/{id}/approve ──────────────────────────

    @Nested
    @DisplayName("POST /api/admin/registrations/{id}/approve — Approve Registration")
    class ApproveRegistration {

        @Test
        @DisplayName("Approving a PENDING registration provisions the tenant and returns tenantId")
        void approve_pending_returnsTenantId() throws Exception {
            UUID regId    = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UUID bizId    = UUID.randomUUID();

            ProvisioningResponse response = new ProvisioningResponse(
                    tenantId, bizId, "alice@example.com",
                    "Tenant provisioned successfully. Credentials sent to alice@example.com");

            Mockito.when(registrationService.approveRegistration(eq(regId), any()))
                    .thenReturn(response);

            RegistrationDecisionRequest body = new RegistrationDecisionRequest();
            body.setAdminNotes("All documents verified.");

            mockMvc.perform(post("/api/admin/registrations/{id}/approve", regId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                    .andExpect(jsonPath("$.businessId").value(bizId.toString()))
                    .andExpect(jsonPath("$.ownerEmail").value("alice@example.com"))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("Approving without a body (no admin notes) still works — body is optional")
        void approve_noBody_returns200() throws Exception {
            UUID regId    = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UUID bizId    = UUID.randomUUID();

            Mockito.when(registrationService.approveRegistration(eq(regId), isNull()))
                    .thenReturn(new ProvisioningResponse(
                            tenantId, bizId, "alice@example.com", "Tenant provisioned successfully."));

            mockMvc.perform(post("/api/admin/registrations/{id}/approve", regId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenantId").value(tenantId.toString()));
        }

        @Test
        @DisplayName("Approving an already-approved registration returns 422")
        void approve_alreadyApproved_returns422() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.approveRegistration(eq(regId), any()))
                    .thenThrow(new BusinessRegistrationException(
                            "Cannot transition registration from APPROVED to APPROVED"));

            mockMvc.perform(post("/api/admin/registrations/{id}/approve", regId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Approving a non-existent registration returns 404")
        void approve_notFound_returns404() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.approveRegistration(eq(regId), any()))
                    .thenThrow(new BusinessRegistrationException("Registration not found: " + regId));

            mockMvc.perform(post("/api/admin/registrations/{id}/approve", regId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/admin/registrations/{id}/reject ────────────────────────────

    @Nested
    @DisplayName("POST /api/admin/registrations/{id}/reject — Reject Registration")
    class RejectRegistration {

        @Test
        @DisplayName("Rejecting a PENDING registration returns REJECTED status")
        void reject_pending_returnsRejected() throws Exception {
            UUID regId = UUID.randomUUID();
            TenantRegistrationDto dto = pendingDto(regId);
            dto.setStatus(ERegistrationStatus.REJECTED);
            dto.setRejectionReason("Incomplete documents.");

            Mockito.when(registrationService.rejectRegistration(eq(regId), eq("Incomplete documents.")))
                    .thenReturn(dto);

            RegistrationDecisionRequest body = new RegistrationDecisionRequest();
            body.setRejectionReason("Incomplete documents.");

            mockMvc.perform(post("/api/admin/registrations/{id}/reject", regId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("Incomplete documents."));
        }

        @Test
        @DisplayName("Rejecting an already-rejected registration returns 422")
        void reject_alreadyRejected_returns422() throws Exception {
            UUID regId = UUID.randomUUID();
            Mockito.when(registrationService.rejectRegistration(eq(regId), any()))
                    .thenThrow(new BusinessRegistrationException(
                            "Cannot transition registration from REJECTED to REJECTED"));

            RegistrationDecisionRequest body = new RegistrationDecisionRequest();
            body.setRejectionReason("Some reason");

            mockMvc.perform(post("/api/admin/registrations/{id}/reject", regId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
