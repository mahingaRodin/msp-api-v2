package com.msp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.msp.enums.EUserRole;
import com.msp.exceptions.PortalException;
import com.msp.payloads.dtos.BranchDto;
import com.msp.payloads.dtos.StoreDto;
import com.msp.payloads.dtos.UserDto;
import com.msp.payloads.request.CreateBranchRequest;
import com.msp.payloads.request.CreateEmployeeRequest;
import com.msp.payloads.request.CreateStoreRequest;
import com.msp.services.BusinessPortalService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusinessPortalController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BusinessPortalController Tests")
class BusinessPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private BusinessPortalService portalService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private StoreDto storeDto(UUID id) {
        StoreDto dto = new StoreDto();
        dto.setId(id);
        dto.setBrand("Alice Retail");
        dto.setStoreType("RETAIL");
        return dto;
    }

    private BranchDto branchDto(UUID id, UUID storeId) {
        BranchDto dto = new BranchDto();
        dto.setId(id);
        dto.setName("Main Branch");
        dto.setStoreId(storeId);
        return dto;
    }

    private UserDto employeeDto(UUID id) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setFirstName("Bob");
        dto.setLastName("Smith");
        dto.setEmail("bob@business.com");
        dto.setRole(EUserRole.ROLE_STORE_MANAGER);
        return dto;
    }

    private CreateStoreRequest storeRequest() {
        CreateStoreRequest req = new CreateStoreRequest();
        req.setBrand("Alice Retail");
        req.setStoreType("RETAIL");
        return req;
    }

    private CreateBranchRequest branchRequest(UUID storeId) {
        CreateBranchRequest req = new CreateBranchRequest();
        req.setName("Main Branch");
        req.setAddress("Kigali");
        req.setStoreId(storeId);
        return req;
    }

    private CreateEmployeeRequest employeeRequest(EUserRole role) {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFirstName("Bob");
        req.setLastName("Smith");
        req.setEmail("bob@business.com");
        req.setPassword("pass123");
        req.setRole(role);
        return req;
    }

    // ── POST /api/portal/business/stores ─────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portal/business/stores — Create Store")
    class CreateStore {

        @Test
        @DisplayName("Valid request returns 201 with store data")
        void create_valid_returns201() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.createStore(any(CreateStoreRequest.class)))
                    .thenReturn(storeDto(storeId));

            mockMvc.perform(post("/api/portal/business/stores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(storeRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(storeId.toString()))
                    .andExpect(jsonPath("$.brand").value("Alice Retail"));
        }

        @Test
        @DisplayName("Missing brand returns 400")
        void create_missingBrand_returns400() throws Exception {
            CreateStoreRequest req = storeRequest();
            req.setBrand(null);

            mockMvc.perform(post("/api/portal/business/stores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Owner not linked to tenant returns 400")
        void create_noTenant_returns400() throws Exception {
            Mockito.when(portalService.createStore(any()))
                    .thenThrow(new PortalException("Your account is not linked to a business tenant."));

            mockMvc.perform(post("/api/portal/business/stores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(storeRequest())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/portal/business/stores ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/portal/business/stores — List My Stores")
    class GetMyStores {

        @Test
        @DisplayName("Returns paginated store list")
        void list_returnsPage() throws Exception {
            UUID storeId = UUID.randomUUID();
            Page<StoreDto> page = new PageImpl<>(List.of(storeDto(storeId)));
            Mockito.when(portalService.getMyStores(0, 10)).thenReturn(page);

            mockMvc.perform(get("/api/portal/business/stores")
                            .param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(storeId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Empty tenant returns empty page")
        void list_empty_returnsEmptyPage() throws Exception {
            Mockito.when(portalService.getMyStores(anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/portal/business/stores"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── PUT /api/portal/business/stores/{storeId} ─────────────────────────────

    @Nested
    @DisplayName("PUT /api/portal/business/stores/{storeId} — Update Store")
    class UpdateStore {

        @Test
        @DisplayName("Valid update returns 200 with updated store")
        void update_valid_returns200() throws Exception {
            UUID storeId = UUID.randomUUID();
            StoreDto updated = storeDto(storeId);
            updated.setBrand("Updated Brand");
            Mockito.when(portalService.updateStore(eq(storeId), any()))
                    .thenReturn(updated);

            CreateStoreRequest req = storeRequest();
            req.setBrand("Updated Brand");

            mockMvc.perform(put("/api/portal/business/stores/{storeId}", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.brand").value("Updated Brand"));
        }

        @Test
        @DisplayName("Store not found returns 404")
        void update_notFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.updateStore(eq(storeId), any()))
                    .thenThrow(new PortalException("Store not found: " + storeId));

            mockMvc.perform(put("/api/portal/business/stores/{storeId}", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(storeRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Store belongs to different tenant returns 403")
        void update_foreignTenant_returns403() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.updateStore(eq(storeId), any()))
                    .thenThrow(new PortalException("Store does not belong to your business tenant."));

            mockMvc.perform(put("/api/portal/business/stores/{storeId}", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(storeRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ── DELETE /api/portal/business/stores/{storeId} ──────────────────────────

    @Nested
    @DisplayName("DELETE /api/portal/business/stores/{storeId} — Delete Store")
    class DeleteStore {

        @Test
        @DisplayName("Existing store returns 200 with message")
        void delete_exists_returns200() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.doNothing().when(portalService).deleteStore(storeId);

            mockMvc.perform(delete("/api/portal/business/stores/{storeId}", storeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Store deleted successfully."));
        }

        @Test
        @DisplayName("Store not found returns 404")
        void delete_notFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.doThrow(new PortalException("Store not found: " + storeId))
                    .when(portalService).deleteStore(storeId);

            mockMvc.perform(delete("/api/portal/business/stores/{storeId}", storeId))
                    .andExpect(status().isNotFound());
        }
    }

    // ── POST /api/portal/business/branches ────────────────────────────────────

    @Nested
    @DisplayName("POST /api/portal/business/branches — Create Branch")
    class CreateBranch {

        @Test
        @DisplayName("Valid request returns 201 with branch data")
        void create_valid_returns201() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            Mockito.when(portalService.createBranch(any(CreateBranchRequest.class)))
                    .thenReturn(branchDto(branchId, storeId));

            mockMvc.perform(post("/api/portal/business/branches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(branchRequest(storeId))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(branchId.toString()))
                    .andExpect(jsonPath("$.name").value("Main Branch"));
        }

        @Test
        @DisplayName("Missing name returns 400")
        void create_missingName_returns400() throws Exception {
            UUID storeId = UUID.randomUUID();
            CreateBranchRequest req = branchRequest(storeId);
            req.setName(null);

            mockMvc.perform(post("/api/portal/business/branches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing storeId returns 400")
        void create_missingStoreId_returns400() throws Exception {
            CreateBranchRequest req = branchRequest(null);

            mockMvc.perform(post("/api/portal/business/branches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Store not found returns 404")
        void create_storeNotFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.createBranch(any()))
                    .thenThrow(new PortalException("Store not found: " + storeId));

            mockMvc.perform(post("/api/portal/business/branches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(branchRequest(storeId))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Store belongs to different tenant returns 403")
        void create_foreignTenant_returns403() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.createBranch(any()))
                    .thenThrow(new PortalException("Store does not belong to your business tenant."));

            mockMvc.perform(post("/api/portal/business/branches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(branchRequest(storeId))))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/portal/business/stores/{storeId}/branches ───────────────────

    @Nested
    @DisplayName("GET /api/portal/business/stores/{storeId}/branches — List Branches")
    class GetStoreBranches {

        @Test
        @DisplayName("Returns paginated branch list for a store")
        void list_returnsPage() throws Exception {
            UUID storeId  = UUID.randomUUID();
            UUID branchId = UUID.randomUUID();
            Page<BranchDto> page = new PageImpl<>(List.of(branchDto(branchId, storeId)));
            Mockito.when(portalService.getStoreBranches(eq(storeId), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/portal/business/stores/{storeId}/branches", storeId)
                            .param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(branchId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Store not found returns 404")
        void list_storeNotFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.getStoreBranches(eq(storeId), anyInt(), anyInt()))
                    .thenThrow(new PortalException("Store not found: " + storeId));

            mockMvc.perform(get("/api/portal/business/stores/{storeId}/branches", storeId))
                    .andExpect(status().isNotFound());
        }
    }

    // ── DELETE /api/portal/business/branches/{branchId} ───────────────────────

    @Nested
    @DisplayName("DELETE /api/portal/business/branches/{branchId} — Delete Branch")
    class DeleteBranch {

        @Test
        @DisplayName("Existing branch returns 200 with message")
        void delete_exists_returns200() throws Exception {
            UUID branchId = UUID.randomUUID();
            Mockito.doNothing().when(portalService).deleteBranch(branchId);

            mockMvc.perform(delete("/api/portal/business/branches/{branchId}", branchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Branch deleted successfully."));
        }

        @Test
        @DisplayName("Branch not found returns 404")
        void delete_notFound_returns404() throws Exception {
            UUID branchId = UUID.randomUUID();
            Mockito.doThrow(new PortalException("Branch not found: " + branchId))
                    .when(portalService).deleteBranch(branchId);

            mockMvc.perform(delete("/api/portal/business/branches/{branchId}", branchId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Branch belongs to different tenant returns 403")
        void delete_foreignTenant_returns403() throws Exception {
            UUID branchId = UUID.randomUUID();
            Mockito.doThrow(new PortalException("Branch does not belong to your business tenant."))
                    .when(portalService).deleteBranch(branchId);

            mockMvc.perform(delete("/api/portal/business/branches/{branchId}", branchId))
                    .andExpect(status().isForbidden());
        }
    }

    // ── POST /api/portal/business/stores/{storeId}/employees ─────────────────

    @Nested
    @DisplayName("POST /api/portal/business/stores/{storeId}/employees — Create Employee")
    class CreateEmployee {

        @Test
        @DisplayName("Valid STORE_MANAGER request returns 201")
        void create_storeManager_returns201() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID empId   = UUID.randomUUID();
            Mockito.when(portalService.createEmployee(eq(storeId), any(CreateEmployeeRequest.class)))
                    .thenReturn(employeeDto(empId));

            mockMvc.perform(post("/api/portal/business/stores/{storeId}/employees", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    employeeRequest(EUserRole.ROLE_STORE_MANAGER))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(empId.toString()))
                    .andExpect(jsonPath("$.email").value("bob@business.com"));
        }

        @Test
        @DisplayName("Missing email returns 400")
        void create_missingEmail_returns400() throws Exception {
            UUID storeId = UUID.randomUUID();
            CreateEmployeeRequest req = employeeRequest(EUserRole.ROLE_STORE_MANAGER);
            req.setEmail(null);

            mockMvc.perform(post("/api/portal/business/stores/{storeId}/employees", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Duplicate email returns 409")
        void create_duplicateEmail_returns409() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.createEmployee(eq(storeId), any()))
                    .thenThrow(new PortalException(
                            "An account with email 'bob@business.com' already exists."));

            mockMvc.perform(post("/api/portal/business/stores/{storeId}/employees", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    employeeRequest(EUserRole.ROLE_STORE_MANAGER))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Store not found returns 404")
        void create_storeNotFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.createEmployee(eq(storeId), any()))
                    .thenThrow(new PortalException("Store not found: " + storeId));

            mockMvc.perform(post("/api/portal/business/stores/{storeId}/employees", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    employeeRequest(EUserRole.ROLE_STORE_MANAGER))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Forbidden role returns 400")
        void create_forbiddenRole_returns400() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.createEmployee(eq(storeId), any()))
                    .thenThrow(new PortalException(
                            "Role ROLE_STORE_ADMIN cannot be assigned to employees."));

            mockMvc.perform(post("/api/portal/business/stores/{storeId}/employees", storeId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    employeeRequest(EUserRole.ROLE_STORE_ADMIN))))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/portal/business/stores/{storeId}/employees ──────────────────

    @Nested
    @DisplayName("GET /api/portal/business/stores/{storeId}/employees — List Employees")
    class GetStoreEmployees {

        @Test
        @DisplayName("Returns paginated employee list for a store")
        void list_returnsPage() throws Exception {
            UUID storeId = UUID.randomUUID();
            UUID empId   = UUID.randomUUID();
            Page<UserDto> page = new PageImpl<>(List.of(employeeDto(empId)));
            Mockito.when(portalService.getStoreEmployees(eq(storeId), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/portal/business/stores/{storeId}/employees", storeId)
                            .param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(empId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Store not found returns 404")
        void list_storeNotFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(portalService.getStoreEmployees(eq(storeId), anyInt(), anyInt()))
                    .thenThrow(new PortalException("Store not found: " + storeId));

            mockMvc.perform(get("/api/portal/business/stores/{storeId}/employees", storeId))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PUT /api/portal/business/employees/{employeeId} ───────────────────────

    @Nested
    @DisplayName("PUT /api/portal/business/employees/{employeeId} — Update Employee")
    class UpdateEmployee {

        @Test
        @DisplayName("Valid update returns 200 with updated employee")
        void update_valid_returns200() throws Exception {
            UUID empId = UUID.randomUUID();
            UserDto updated = employeeDto(empId);
            updated.setFirstName("Bobby");
            Mockito.when(portalService.updateEmployee(eq(empId), any()))
                    .thenReturn(updated);

            CreateEmployeeRequest req = employeeRequest(EUserRole.ROLE_STORE_MANAGER);
            req.setFirstName("Bobby");

            mockMvc.perform(put("/api/portal/business/employees/{employeeId}", empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Bobby"));
        }

        @Test
        @DisplayName("Employee not found returns 404")
        void update_notFound_returns404() throws Exception {
            UUID empId = UUID.randomUUID();
            Mockito.when(portalService.updateEmployee(eq(empId), any()))
                    .thenThrow(new PortalException("Employee not found: " + empId));

            mockMvc.perform(put("/api/portal/business/employees/{employeeId}", empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    employeeRequest(EUserRole.ROLE_STORE_MANAGER))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Employee belongs to different tenant returns 403")
        void update_foreignTenant_returns403() throws Exception {
            UUID empId = UUID.randomUUID();
            Mockito.when(portalService.updateEmployee(eq(empId), any()))
                    .thenThrow(new PortalException("Employee does not belong to your business tenant."));

            mockMvc.perform(put("/api/portal/business/employees/{employeeId}", empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    employeeRequest(EUserRole.ROLE_STORE_MANAGER))))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PATCH /api/portal/business/employees/{employeeId}/discharge ───────────

    @Nested
    @DisplayName("PATCH /api/portal/business/employees/{employeeId}/discharge — Discharge")
    class DischargeEmployee {

        @Test
        @DisplayName("Active employee is discharged and returns 200")
        void discharge_active_returns200() throws Exception {
            UUID empId = UUID.randomUUID();
            UserDto discharged = employeeDto(empId);
            Mockito.when(portalService.dischargeEmployee(empId)).thenReturn(discharged);

            mockMvc.perform(patch("/api/portal/business/employees/{employeeId}/discharge", empId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(empId.toString()));
        }

        @Test
        @DisplayName("Already discharged employee returns 400")
        void discharge_alreadyDischarged_returns400() throws Exception {
            UUID empId = UUID.randomUUID();
            Mockito.when(portalService.dischargeEmployee(empId))
                    .thenThrow(new PortalException("Employee is already discharged."));

            mockMvc.perform(patch("/api/portal/business/employees/{employeeId}/discharge", empId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Employee not found returns 404")
        void discharge_notFound_returns404() throws Exception {
            UUID empId = UUID.randomUUID();
            Mockito.when(portalService.dischargeEmployee(empId))
                    .thenThrow(new PortalException("Employee not found: " + empId));

            mockMvc.perform(patch("/api/portal/business/employees/{employeeId}/discharge", empId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Employee belongs to different tenant returns 403")
        void discharge_foreignTenant_returns403() throws Exception {
            UUID empId = UUID.randomUUID();
            Mockito.when(portalService.dischargeEmployee(empId))
                    .thenThrow(new PortalException("Employee does not belong to your business tenant."));

            mockMvc.perform(patch("/api/portal/business/employees/{employeeId}/discharge", empId))
                    .andExpect(status().isForbidden());
        }
    }
}
