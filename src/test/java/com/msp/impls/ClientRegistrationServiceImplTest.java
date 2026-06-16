package com.msp.impls;

import com.msp.enums.EUserRole;
import com.msp.exceptions.CustomerException;
import com.msp.models.Customer;
import com.msp.models.CustomerStoreRelationship;
import com.msp.models.Store;
import com.msp.payloads.dtos.CustomerDto;
import com.msp.payloads.dtos.CustomerStoreRelationshipDto;
import com.msp.payloads.dtos.CustomerUpdateDto;
import com.msp.payloads.request.CustomerRegistrationRequest;
import com.msp.payloads.response.CustomerRegistrationResponse;
import com.msp.repositories.CustomerRepository;
import com.msp.repositories.CustomerStoreRelationshipRepository;
import com.msp.repositories.StoreRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerRegistrationServiceImpl Tests")
class ClientRegistrationServiceImplTest {

    @Mock private CustomerRepository customerRepo;
    @Mock private CustomerStoreRelationshipRepository relationshipRepo;
    @Mock private StoreRepository storeRepo;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientRegistrationServiceImpl service;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Customer customer(UUID id) {
        Customer c = new Customer();
        c.setId(id);
        c.setFirstName("John");
        c.setLastName("Doe");
        c.setEmail("john@example.com");
        c.setPhone("+250788111222");
        c.setRole(EUserRole.ROLE_CUSTOMER);
        c.setPassword("$2a$10$hashed");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    private Store store(UUID id) {
        Store s = new Store();
        s.setId(id);
        s.setBrand("Alice Retail Ltd");
        return s;
    }

    private CustomerStoreRelationship relationship(Customer c, Store s) {
        CustomerStoreRelationship rel = new CustomerStoreRelationship();
        rel.setId(UUID.randomUUID());
        rel.setCustomer(c);
        rel.setStore(s);
        rel.setFirstInteractionAt(LocalDateTime.now());
        rel.setLastInteractionAt(LocalDateTime.now());
        return rel;
    }

    private CustomerRegistrationRequest validRequest() {
        CustomerRegistrationRequest req = new CustomerRegistrationRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john@example.com");
        req.setPhone("+250788111222");
        req.setPassword("secret123");
        return req;
    }

    // ── register() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Happy path — saves customer with hashed password and ROLE_CUSTOMER, no store link")
        void register_happyPath_savesGlobalAccount() {
            UUID customerId = UUID.randomUUID();
            when(customerRepo.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashed");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            when(customerRepo.save(captor.capture())).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setId(customerId);
                return c;
            });

            CustomerRegistrationResponse result = service.register(validRequest());

            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getMessage()).contains("browse and shop");

            Customer saved = captor.getValue();
            assertThat(saved.getRole()).isEqualTo(EUserRole.ROLE_CUSTOMER);
            assertThat(saved.getPassword()).isEqualTo("$2a$10$hashed");
            assertThat(saved.getCreatedAt()).isNotNull();
            // No store link on the Customer entity itself
        }

        @Test
        @DisplayName("Password is encoded — plain text never stored")
        void register_passwordIsEncoded() {
            when(customerRepo.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            when(customerRepo.save(captor.capture())).thenAnswer(inv -> {
                Customer c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            service.register(validRequest());

            assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(captor.getValue().getPassword()).doesNotContain("secret123");
        }

        @Test
        @DisplayName("Throws 409 when email already exists globally")
        void register_duplicateEmail_throws() {
            when(customerRepo.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register(validRequest()))
                    .isInstanceOf(CustomerException.class)
                    .hasMessageContaining("already exists");

            verify(customerRepo, never()).save(any());
        }
    }

    // ── getCustomer() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomer()")
    class GetCustomer {

        @Test
        @DisplayName("Returns DTO for existing customer")
        void get_exists_returnsDto() {
            UUID id = UUID.randomUUID();
            when(customerRepo.findById(id)).thenReturn(Optional.of(customer(id)));

            CustomerDto result = service.getCustomer(id);

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Throws for non-existent customer")
        void get_notFound_throws() {
            UUID id = UUID.randomUUID();
            when(customerRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomer(id))
                    .isInstanceOf(CustomerException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── getMyStores() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyStores()")
    class GetMyStores {

        @Test
        @DisplayName("Returns stores the customer has interacted with")
        void getMyStores_returnsRelationships() {
            UUID customerId = UUID.randomUUID();
            UUID storeId    = UUID.randomUUID();
            Customer c = customer(customerId);
            Store s    = store(storeId);

            when(customerRepo.findById(customerId)).thenReturn(Optional.of(c));
            Page<CustomerStoreRelationship> page =
                    new PageImpl<>(List.of(relationship(c, s)));
            when(relationshipRepo.findByCustomer(eq(c), any())).thenReturn(page);

            Page<CustomerStoreRelationshipDto> result = service.getMyStores(customerId, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(customerId);
            assertThat(result.getContent().get(0).getStoreId()).isEqualTo(storeId);
        }
    }

    // ── getCustomersByStore() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomersByStore()")
    class GetByStore {

        @Test
        @DisplayName("Returns only customers who have interacted with the store")
        void list_returnsStoreCustomers() {
            UUID storeId    = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Store s = store(storeId);
            Customer c = customer(customerId);

            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            Page<CustomerStoreRelationship> page =
                    new PageImpl<>(List.of(relationship(c, s)));
            when(relationshipRepo.findByStore(eq(s), any())).thenReturn(page);

            Page<CustomerStoreRelationshipDto> result =
                    service.getCustomersByStore(storeId, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getStoreId()).isEqualTo(storeId);
        }

        @Test
        @DisplayName("Throws when store not found")
        void list_storeNotFound_throws() {
            UUID storeId = UUID.randomUUID();
            when(storeRepo.findById(storeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomersByStore(storeId, 0, 10))
                    .isInstanceOf(CustomerException.class)
                    .hasMessageContaining("Store not found");
        }
    }

    // ── getCustomerInStore() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerInStore()")
    class GetCustomerInStore {

        @Test
        @DisplayName("Returns relationship DTO when customer has interacted with store")
        void get_relationshipExists_returnsDto() {
            UUID customerId = UUID.randomUUID();
            UUID storeId    = UUID.randomUUID();
            Customer c = customer(customerId);
            Store s    = store(storeId);

            when(customerRepo.findById(customerId)).thenReturn(Optional.of(c));
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(relationshipRepo.findByCustomerAndStore(c, s))
                    .thenReturn(Optional.of(relationship(c, s)));

            CustomerStoreRelationshipDto result =
                    service.getCustomerInStore(customerId, storeId);

            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getStoreId()).isEqualTo(storeId);
        }

        @Test
        @DisplayName("Throws when customer has never interacted with the store")
        void get_noRelationship_throws() {
            UUID customerId = UUID.randomUUID();
            UUID storeId    = UUID.randomUUID();
            Customer c = customer(customerId);
            Store s    = store(storeId);

            when(customerRepo.findById(customerId)).thenReturn(Optional.of(c));
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(relationshipRepo.findByCustomerAndStore(c, s)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCustomerInStore(customerId, storeId))
                    .isInstanceOf(CustomerException.class)
                    .hasMessageContaining("not interacted");
        }
    }

    // ── ensureRelationship() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("ensureRelationship()")
    class EnsureRelationship {

        @Test
        @DisplayName("Creates new relationship when none exists (first order at store)")
        void ensure_noExisting_createsNew() {
            UUID customerId = UUID.randomUUID();
            UUID storeId    = UUID.randomUUID();
            Customer c = customer(customerId);
            Store s    = store(storeId);

            when(customerRepo.findById(customerId)).thenReturn(Optional.of(c));
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(relationshipRepo.findByCustomerAndStore(c, s)).thenReturn(Optional.empty());

            CustomerStoreRelationship newRel = relationship(c, s);
            when(relationshipRepo.save(any())).thenReturn(newRel);

            CustomerStoreRelationship result = service.ensureRelationship(customerId, storeId);

            assertThat(result).isNotNull();
            assertThat(result.getCustomer()).isEqualTo(c);
            assertThat(result.getStore()).isEqualTo(s);
            verify(relationshipRepo).save(any());
        }

        @Test
        @DisplayName("Returns existing relationship without creating a duplicate (idempotent)")
        void ensure_alreadyExists_returnsExisting() {
            UUID customerId = UUID.randomUUID();
            UUID storeId    = UUID.randomUUID();
            Customer c = customer(customerId);
            Store s    = store(storeId);
            CustomerStoreRelationship existing = relationship(c, s);

            when(customerRepo.findById(customerId)).thenReturn(Optional.of(c));
            when(storeRepo.findById(storeId)).thenReturn(Optional.of(s));
            when(relationshipRepo.findByCustomerAndStore(c, s))
                    .thenReturn(Optional.of(existing));

            CustomerStoreRelationship result = service.ensureRelationship(customerId, storeId);

            assertThat(result).isEqualTo(existing);
            // Must NOT call save — relationship already exists
            verify(relationshipRepo, never()).save(any());
        }
    }

    // ── updateCustomer() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCustomer()")
    class UpdateCustomer {

        @Test
        @DisplayName("Updates name and phone on global profile")
        void update_nameAndPhone_succeeds() {
            UUID id = UUID.randomUUID();
            Customer c = customer(id);
            when(customerRepo.findById(id)).thenReturn(Optional.of(c));
            when(customerRepo.save(any())).thenReturn(c);

            CustomerUpdateDto dto = new CustomerUpdateDto();
            dto.setFirstName("Johnny");
            dto.setPhone("+250788999000");

            service.updateCustomer(id, dto);

            verify(customerRepo).save(argThat(saved ->
                    saved.getFirstName().equals("Johnny")
                    && saved.getPhone().equals("+250788999000")));
        }

        @Test
        @DisplayName("Email change succeeds when new email is globally unique")
        void update_emailChange_unique_succeeds() {
            UUID id = UUID.randomUUID();
            Customer c = customer(id);
            when(customerRepo.findById(id)).thenReturn(Optional.of(c));
            when(customerRepo.existsByEmail("new@example.com")).thenReturn(false);
            when(customerRepo.save(any())).thenReturn(c);

            CustomerUpdateDto dto = new CustomerUpdateDto();
            dto.setEmail("new@example.com");

            assertThatCode(() -> service.updateCustomer(id, dto)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Email change throws when new email already taken globally")
        void update_emailChange_duplicate_throws() {
            UUID id = UUID.randomUUID();
            Customer c = customer(id);
            when(customerRepo.findById(id)).thenReturn(Optional.of(c));
            when(customerRepo.existsByEmail("taken@example.com")).thenReturn(true);

            CustomerUpdateDto dto = new CustomerUpdateDto();
            dto.setEmail("taken@example.com");

            assertThatThrownBy(() -> service.updateCustomer(id, dto))
                    .isInstanceOf(CustomerException.class)
                    .hasMessageContaining("already in use");
        }

        @Test
        @DisplayName("Setting same email does not trigger uniqueness check")
        void update_sameEmail_noCheck() {
            UUID id = UUID.randomUUID();
            Customer c = customer(id);   // email = "john@example.com"
            when(customerRepo.findById(id)).thenReturn(Optional.of(c));
            when(customerRepo.save(any())).thenReturn(c);

            CustomerUpdateDto dto = new CustomerUpdateDto();
            dto.setEmail("john@example.com");

            service.updateCustomer(id, dto);

            verify(customerRepo, never()).existsByEmail(anyString());
        }
    }

    // ── deleteCustomer() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCustomer()")
    class DeleteCustomer {

        @Test
        @DisplayName("Deletes customer and all their store relationships")
        void delete_exists_deletesCustomerAndRelationships() {
            UUID id = UUID.randomUUID();
            Customer c = customer(id);
            when(customerRepo.findById(id)).thenReturn(Optional.of(c));
            when(relationshipRepo.findByCustomer(eq(c), any()))
                    .thenReturn(Page.empty());
            doNothing().when(customerRepo).delete(c);

            service.deleteCustomer(id);

            verify(customerRepo).delete(c);
        }

        @Test
        @DisplayName("Throws when customer not found")
        void delete_notFound_throws() {
            UUID id = UUID.randomUUID();
            when(customerRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCustomer(id))
                    .isInstanceOf(CustomerException.class)
                    .hasMessageContaining("not found");

            verify(customerRepo, never()).delete(any());
        }
    }
}
