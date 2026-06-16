package com.msp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.exceptions.CustomerException;
import com.msp.payloads.dtos.CustomerDto;
import com.msp.payloads.dtos.CustomerStoreRelationshipDto;
import com.msp.payloads.dtos.CustomerUpdateDto;
import com.msp.payloads.request.CustomerRegistrationRequest;
import com.msp.payloads.response.CustomerRegistrationResponse;
import com.msp.services.ClientRegistrationService;
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

@WebMvcTest(ClientRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClientRegistrationController Tests")
class ClientRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ClientRegistrationService clientRegistrationService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /** Valid registration request — no storeId (global account). */
    private CustomerRegistrationRequest validRequest() {
        CustomerRegistrationRequest req = new CustomerRegistrationRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john@example.com");
        req.setPhone("+250788111222");
        req.setPassword("secret123");
        return req;
    }

    /** Global customer DTO — no storeId field. */
    private CustomerDto customerDto(UUID id) {
        CustomerDto dto = new CustomerDto();
        dto.setId(id);
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john@example.com");
        dto.setPhone("+250788111222");
        return dto;
    }

    /** Store-relationship DTO — what the store portal returns. */
    private CustomerStoreRelationshipDto relationshipDto(UUID customerId, UUID storeId) {
        CustomerStoreRelationshipDto dto = new CustomerStoreRelationshipDto();
        dto.setRelationshipId(UUID.randomUUID());
        dto.setCustomerId(customerId);
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john@example.com");
        dto.setStoreId(storeId);
        dto.setStoreName("Alice Retail Ltd");
        dto.setFirstInteractionAt(LocalDateTime.now());
        dto.setLastInteractionAt(LocalDateTime.now());
        return dto;
    }

    // ── POST /api/clients/register ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/clients/register — Global Self Registration")
    class Register {

        @Test
        @DisplayName("Valid request returns 201 Created with customerId and message")
        void register_valid_returns201() throws Exception {
            UUID customerId = UUID.randomUUID();

            Mockito.when(clientRegistrationService.register(any(CustomerRegistrationRequest.class)))
                    .thenReturn(new CustomerRegistrationResponse(
                            customerId,
                            "john@example.com",
                            "Account created successfully. You can now browse and shop from any store."));

            mockMvc.perform(post("/api/clients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("Missing firstName returns 400")
        void register_missingFirstName_returns400() throws Exception {
            CustomerRegistrationRequest req = validRequest();
            req.setFirstName(null);

            mockMvc.perform(post("/api/clients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Missing lastName returns 400")
        void register_missingLastName_returns400() throws Exception {
            CustomerRegistrationRequest req = validRequest();
            req.setLastName(null);

            mockMvc.perform(post("/api/clients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Invalid email format returns 400")
        void register_invalidEmail_returns400() throws Exception {
            CustomerRegistrationRequest req = validRequest();
            req.setEmail("not-an-email");

            mockMvc.perform(post("/api/clients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Password shorter than 6 chars returns 400")
        void register_shortPassword_returns400() throws Exception {
            CustomerRegistrationRequest req = validRequest();
            req.setPassword("abc");

            mockMvc.perform(post("/api/clients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Duplicate email returns 409 Conflict")
        void register_duplicateEmail_returns409() throws Exception {
            Mockito.when(clientRegistrationService.register(any()))
                    .thenThrow(new CustomerException(
                            "An account with email 'john@example.com' already exists."));

            mockMvc.perform(post("/api/clients/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("An account with email 'john@example.com' already exists."));
        }
    }

    // ── GET /api/clients/{id}/profile ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients/{id}/profile — Get My Profile")
    class GetProfile {

        @Test
        @DisplayName("Existing customer returns 200 with global profile")
        void get_exists_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            Mockito.when(clientRegistrationService.getCustomer(id))
                    .thenReturn(customerDto(id));

            mockMvc.perform(get("/api/clients/{id}/profile", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Non-existent customer returns 404")
        void get_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            Mockito.when(clientRegistrationService.getCustomer(id))
                    .thenThrow(new CustomerException("Customer not found: " + id));

            mockMvc.perform(get("/api/clients/{id}/profile", id))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/clients/{id}/stores ───────────────────────────────────────

    @Nested
    @DisplayName("GET /api/clients/{id}/stores — Get My Stores")
    class GetMyStores {

        @Test
        @DisplayName("Returns paginated list of stores the customer has shopped at")
        void getMyStores_returnsPage() throws Exception {
            UUID customerId = UUID.randomUUID();
            UUID storeId    = UUID.randomUUID();

            Page<CustomerStoreRelationshipDto> page =
                    new PageImpl<>(List.of(relationshipDto(customerId, storeId)));

            Mockito.when(clientRegistrationService.getMyStores(eq(customerId), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/clients/{id}/stores", customerId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.content[0].storeId").value(storeId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Customer with no orders returns empty page")
        void getMyStores_noOrders_returnsEmpty() throws Exception {
            UUID customerId = UUID.randomUUID();
            Mockito.when(clientRegistrationService.getMyStores(eq(customerId), anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/clients/{id}/stores", customerId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── PATCH /api/clients/{id}/profile ────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/clients/{id}/profile — Update My Profile")
    class UpdateProfile {

        @Test
        @DisplayName("Valid update returns 200 with updated data")
        void update_valid_returns200() throws Exception {
            UUID id = UUID.randomUUID();

            CustomerUpdateDto dto = new CustomerUpdateDto();
            dto.setFirstName("Johnny");
            dto.setPhone("+250788999000");

            CustomerDto updated = customerDto(id);
            updated.setFirstName("Johnny");

            Mockito.when(clientRegistrationService.updateCustomer(eq(id), any(CustomerUpdateDto.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch("/api/clients/{id}/profile", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Johnny"));
        }

        @Test
        @DisplayName("Duplicate email returns 409")
        void update_duplicateEmail_returns409() throws Exception {
            UUID id = UUID.randomUUID();
            CustomerUpdateDto dto = new CustomerUpdateDto();
            dto.setEmail("taken@example.com");

            Mockito.when(clientRegistrationService.updateCustomer(eq(id), any()))
                    .thenThrow(new CustomerException("Email 'taken@example.com' is already in use."));

            mockMvc.perform(patch("/api/clients/{id}/profile", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Customer not found returns 404")
        void update_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            Mockito.when(clientRegistrationService.updateCustomer(eq(id), any()))
                    .thenThrow(new CustomerException("Customer not found: " + id));

            mockMvc.perform(patch("/api/clients/{id}/profile", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── GET /api/clients/portal/stores/{storeId} ───────────────────────────

    @Nested
    @DisplayName("GET /api/clients/portal/stores/{storeId} — Store Portal: List Customers")
    class GetStoreCustomers {

        @Test
        @DisplayName("Returns paginated relationship DTOs for the store")
        void list_returnsPage() throws Exception {
            UUID storeId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            Page<CustomerStoreRelationshipDto> page =
                    new PageImpl<>(List.of(relationshipDto(customerId, storeId)));

            Mockito.when(clientRegistrationService.getCustomersByStore(eq(storeId), eq(0), eq(10)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/clients/portal/stores/{storeId}", storeId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.content[0].storeName").value("Alice Retail Ltd"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Store not found returns 404")
        void list_storeNotFound_returns404() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(clientRegistrationService.getCustomersByStore(eq(storeId), anyInt(), anyInt()))
                    .thenThrow(new CustomerException("Store not found: " + storeId));

            mockMvc.perform(get("/api/clients/portal/stores/{storeId}", storeId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Store with no clients returns empty page")
        void list_empty_returnsEmptyPage() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(clientRegistrationService.getCustomersByStore(eq(storeId), anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/clients/portal/stores/{storeId}", storeId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── GET /api/clients/portal/stores/{storeId}/search ────────────────────

    @Nested
    @DisplayName("GET /api/clients/portal/stores/{storeId}/search — Store Portal: Search")
    class SearchStoreCustomers {

        @Test
        @DisplayName("Returns matching relationship DTOs for keyword")
        void search_returnsMatches() throws Exception {
            UUID storeId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            Page<CustomerStoreRelationshipDto> page =
                    new PageImpl<>(List.of(relationshipDto(customerId, storeId)));

            Mockito.when(clientRegistrationService.searchCustomersByStore(
                            eq(storeId), eq("john"), anyInt(), anyInt()))
                    .thenReturn(page);

            mockMvc.perform(get("/api/clients/portal/stores/{storeId}/search", storeId)
                            .param("q", "john"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()));
        }

        @Test
        @DisplayName("No matches returns 200 with empty content")
        void search_noMatches_returnsEmpty() throws Exception {
            UUID storeId = UUID.randomUUID();
            Mockito.when(clientRegistrationService.searchCustomersByStore(
                            eq(storeId), eq("xyz"), anyInt(), anyInt()))
                    .thenReturn(Page.empty());

            mockMvc.perform(get("/api/clients/portal/stores/{storeId}/search", storeId)
                            .param("q", "xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── GET /api/clients/stores/{storeId}/{clientId} ──────────────

    @Nested
    @DisplayName("GET /api/clients/stores/{storeId}/{clientId} — Store Portal: Get One")
    class GetCustomerInStore {

        @Test
        @DisplayName("Returns relationship DTO when customer has interacted with store")
        void get_exists_returns200() throws Exception {
            UUID storeId    = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            Mockito.when(clientRegistrationService.getCustomerInStore(clientId, storeId))
                    .thenReturn(relationshipDto(clientId, storeId));

            mockMvc.perform(get("/api/clients/stores/{storeId}/{clientId}",
                            storeId, clientId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value(clientId.toString()))
                    .andExpect(jsonPath("$.storeId").value(storeId.toString()));
        }

        @Test
        @DisplayName("Customer has never interacted with store returns 404")
        void get_noRelationship_returns404() throws Exception {
            UUID storeId    = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            Mockito.when(clientRegistrationService.getCustomerInStore(clientId, storeId))
                    .thenThrow(new CustomerException("Customer has not interacted with this store yet."));

            mockMvc.perform(get("/api/clients/stores/{storeId}/{clientId}",
                            storeId, clientId))
                    .andExpect(status().isNotFound());
        }
    }

    // ── PATCH /api/clients/portal/stores/{storeId}/{clientId}/notes ───────

    @Nested
    @DisplayName("PATCH /api/clients/portal/stores/{storeId}/{clientId}/notes — Update Notes")
    class UpdateStoreNotes {

        @Test
        @DisplayName("Updates notes and returns updated relationship DTO")
        void updateNotes_valid_returns200() throws Exception {
            UUID storeId    = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            CustomerStoreRelationshipDto dto = relationshipDto(clientId, storeId);
            dto.setNotes("VIP customer");

            Mockito.when(clientRegistrationService.updateStoreNotes(clientId, storeId, "VIP customer"))
                    .thenReturn(dto);

            mockMvc.perform(patch("/api/clients/portal/stores/{storeId}/{clientId}/notes",
                            storeId, clientId)
                            .param("notes", "VIP customer"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notes").value("VIP customer"));
        }

        @Test
        @DisplayName("Customer not in store returns 404")
        void updateNotes_noRelationship_returns404() throws Exception {
            UUID storeId    = UUID.randomUUID();
            UUID clientId = UUID.randomUUID();

            Mockito.when(clientRegistrationService.updateStoreNotes(clientId, storeId, "note"))
                    .thenThrow(new CustomerException("Customer has not interacted with this store yet."));

            mockMvc.perform(patch("/api/clients/portal/stores/{storeId}/{clientId}/notes",
                            storeId, clientId)
                            .param("notes", "note"))
                    .andExpect(status().isNotFound());
        }
    }

    // ── DELETE /api/clients/{id} ────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/clients/{id} — Super Admin: Delete Account")
    class DeleteCustomer {

        @Test
        @DisplayName("Existing customer is deleted and returns 200 with message")
        void delete_exists_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            Mockito.doNothing().when(clientRegistrationService).deleteCustomer(id);

            mockMvc.perform(delete("/api/clients/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Customer account deleted successfully."));
        }

        @Test
        @DisplayName("Non-existent customer returns 404")
        void delete_notFound_returns404() throws Exception {
            UUID id = UUID.randomUUID();
            Mockito.doThrow(new CustomerException("Customer not found: " + id))
                    .when(clientRegistrationService).deleteCustomer(id);

            mockMvc.perform(delete("/api/clients/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }
}