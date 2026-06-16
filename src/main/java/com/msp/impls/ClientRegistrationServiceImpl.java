package com.msp.impls;

import com.msp.enums.EUserRole;
import com.msp.exceptions.CustomerException;
import com.msp.mappers.CustomerMapper;
import com.msp.mappers.CustomerStoreRelationshipMapper;
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
import com.msp.services.ClientRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "customers")
public class ClientRegistrationServiceImpl implements ClientRegistrationService {

    private final CustomerRepository customerRepo;
    private final CustomerStoreRelationshipRepository relationshipRepo;
    private final StoreRepository storeRepo;
    private final PasswordEncoder passwordEncoder;

    // ── Global self-registration ─────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "customers-all", allEntries = true)
    public CustomerRegistrationResponse register(CustomerRegistrationRequest request) {
        // Email must be globally unique across the entire platform
        if (customerRepo.existsByEmail(request.getEmail())) {
            throw new CustomerException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(EUserRole.ROLE_CUSTOMER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customer = customerRepo.save(customer);

        log.info("Customer registered globally: id={}, email={}", customer.getId(), customer.getEmail());

        return new CustomerRegistrationResponse(
                customer.getId(),
                customer.getEmail(),
                "Account created successfully. You can now browse and shop from any store."
        );
    }

    // ── Customer's own profile ───────────────────────────────────────────────

    @Override
    @Cacheable(key = "#customerId")
    public CustomerDto getCustomer(UUID customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        return CustomerMapper.toDto(customer);
    }

    @Override
    @Cacheable(value = "customer-stores", key = "#customerId + '-' + #page + '-' + #size")
    public Page<CustomerStoreRelationshipDto> getMyStores(UUID customerId, int page, int size) {
        Customer customer = findCustomerOrThrow(customerId);
        Pageable pageable = PageRequest.of(page, size);
        return relationshipRepo.findByCustomer(customer, pageable)
                .map(CustomerStoreRelationshipMapper::toDto);
    }

    // ── Store portal operations ──────────────────────────────────────────────

    @Override
    @Cacheable(value = "customers-store", key = "#storeId + '-' + #page + '-' + #size")
    public Page<CustomerStoreRelationshipDto> getCustomersByStore(UUID storeId, int page, int size) {
        Store store = findStoreOrThrow(storeId);
        Pageable pageable = PageRequest.of(page, size);
        return relationshipRepo.findByStore(store, pageable)
                .map(CustomerStoreRelationshipMapper::toDto);
    }

    @Override
    @Cacheable(value = "customers-search",
               key = "#storeId + '-' + #keyword + '-' + #page + '-' + #size")
    public Page<CustomerStoreRelationshipDto> searchCustomersByStore(
            UUID storeId, String keyword, int page, int size) {
        Store store = findStoreOrThrow(storeId);
        Pageable pageable = PageRequest.of(page, size);
        return relationshipRepo.searchByStoreAndKeyword(store, keyword, pageable)
                .map(CustomerStoreRelationshipMapper::toDto);
    }

    @Override
    public CustomerStoreRelationshipDto getCustomerInStore(UUID customerId, UUID storeId) {
        Customer customer = findCustomerOrThrow(customerId);
        Store store = findStoreOrThrow(storeId);
        CustomerStoreRelationship rel = relationshipRepo.findByCustomerAndStore(customer, store)
                .orElseThrow(() -> new CustomerException(
                        "Customer has not interacted with this store yet."));
        return CustomerStoreRelationshipMapper.toDto(rel);
    }

    @Override
    @Transactional
    public CustomerStoreRelationshipDto updateStoreNotes(
            UUID customerId, UUID storeId, String notes) {
        Customer customer = findCustomerOrThrow(customerId);
        Store store = findStoreOrThrow(storeId);
        CustomerStoreRelationship rel = relationshipRepo.findByCustomerAndStore(customer, store)
                .orElseThrow(() -> new CustomerException(
                        "Customer has not interacted with this store yet."));
        rel.setNotes(notes);
        return CustomerStoreRelationshipMapper.toDto(relationshipRepo.save(rel));
    }

    // ── Customer updates their own profile ───────────────────────────────────

    @Override
    @Transactional
    @Caching(
            put   = { @CachePut(key = "#customerId") },
            evict = { @CacheEvict(value = "customers-all", allEntries = true) }
    )
    public CustomerDto updateCustomer(UUID customerId, CustomerUpdateDto dto) {
        Customer existing = findCustomerOrThrow(customerId);

        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName()  != null) existing.setLastName(dto.getLastName());
        if (dto.getPhone()     != null) existing.setPhone(dto.getPhone());

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(existing.getEmail())) {
            if (customerRepo.existsByEmail(dto.getEmail())) {
                throw new CustomerException(
                        "Email '" + dto.getEmail() + "' is already in use.");
            }
            existing.setEmail(dto.getEmail());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return CustomerMapper.toDto(customerRepo.save(existing));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "#customerId"),
            @CacheEvict(value = "customers-all",    allEntries = true),
            @CacheEvict(value = "customers-store",  allEntries = true),
            @CacheEvict(value = "customers-search", allEntries = true),
            @CacheEvict(value = "customer-stores",  allEntries = true)
    })
    public void deleteCustomer(UUID customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        // Cascade: all CustomerStoreRelationship rows are deleted first
        relationshipRepo.findByCustomer(customer, PageRequest.of(0, Integer.MAX_VALUE))
                .forEach(rel -> relationshipRepo.delete(rel));
        customerRepo.delete(customer);
        log.info("Customer account deleted: id={}, email={}", customerId, customer.getEmail());
    }

    // ── Relationship management (called by OrderService on first order) ───────

    @Override
    @Transactional
    public CustomerStoreRelationship ensureRelationship(UUID customerId, UUID storeId) {
        Customer customer = findCustomerOrThrow(customerId);
        Store store = findStoreOrThrow(storeId);

        return relationshipRepo.findByCustomerAndStore(customer, store)
                .orElseGet(() -> {
                    CustomerStoreRelationship rel = CustomerStoreRelationship.builder()
                            .customer(customer)
                            .store(store)
                            .build();
                    CustomerStoreRelationship saved = relationshipRepo.save(rel);
                    log.info("New customer-store relationship created: customerId={}, storeId={}",
                            customerId, storeId);
                    return saved;
                });
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Customer findCustomerOrThrow(UUID id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> new CustomerException("Customer not found: " + id));
    }

    private Store findStoreOrThrow(UUID id) {
        return storeRepo.findById(id)
                .orElseThrow(() -> new CustomerException("Store not found: " + id));
    }
}
