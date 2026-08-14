package com.msp.configs;

import com.msp.enums.EBusinessStatus;
import com.msp.enums.EStoreStatus;
import com.msp.enums.ESubscriptionTier;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.models.Branch;
import com.msp.models.Business;
import com.msp.models.Category;
import com.msp.models.Inventory;
import com.msp.models.Product;
import com.msp.models.Store;
import com.msp.models.StoreContact;
import com.msp.models.User;
import com.msp.repositories.BranchRepository;
import com.msp.repositories.BusinessRepository;
import com.msp.repositories.CategoryRepository;
import com.msp.repositories.InventoryRepository;
import com.msp.repositories.ProductRepository;
import com.msp.repositories.StoreRepository;
import com.msp.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Demo catalog so every role can walk the UI without manual setup.
 * Enable with DEMO_SEED=true / app.demo.seed=true.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Demo!123";
    private static final String MANAGER_EMAIL = "manager@posify.demo";

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(MANAGER_EMAIL) != null) {
            log.info("Demo data already present — skipping seed");
            return;
        }

        log.info("=== Seeding demo tenant (Kigali Fresh Mart) ===");
        String encoded = passwordEncoder.encode(DEMO_PASSWORD);
        UUID tenantId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        User owner = saveUser("Aline", "Uwase", MANAGER_EMAIL, "+250780000001",
                EUserRole.ROLE_STORE_MANAGER, tenantId, encoded, now);

        Business business = Business.builder()
                .tenantId(tenantId)
                .businessName("Kigali Fresh Mart")
                .legalName("Kigali Fresh Mart Ltd")
                .country("RW")
                .industry("Retail")
                .description("Demo supermarket for POSify walkthroughs")
                .subscriptionTier(ESubscriptionTier.PREMIUM)
                .status(EBusinessStatus.ACTIVE)
                .owner(owner)
                .build();
        businessRepository.save(business);

        Store store = new Store();
        store.setBrand("Kigali Fresh Mart");
        store.setDescription("Flagship grocery tenant used for demos");
        store.setStoreType("SUPERMARKET");
        store.setStoreAdmin(owner);
        store.setTenantId(tenantId);
        store.setContact(StoreContact.builder()
                .address("KN 4 Ave, Kigali")
                .phone("+250780000010")
                .email("hello@posify.demo")
                .build());
        store = storeRepository.save(store);
        store.setStatus(EStoreStatus.ACTIVE);
        store = storeRepository.save(store);

        owner.setStore(store);
        userRepository.save(owner);

        Branch branch = Branch.builder()
                .name("Downtown Kigali")
                .address("KN 4 Ave, Nyarugenge")
                .phone("+250780000011")
                .email("downtown@posify.demo")
                .workingDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"))
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(21, 0))
                .store(store)
                .tenantId(tenantId)
                .build();
        branch = branchRepository.save(branch);

        User branchMgr = saveUser("Jean", "Mugisha", "branch@posify.demo", "+250780000002",
                EUserRole.ROLE_BRANCH_MANAGER, tenantId, encoded, now);
        branchMgr.setStore(store);
        branchMgr.setBranch(branch);
        userRepository.save(branchMgr);
        branch.setManager(branchMgr);
        branchRepository.save(branch);

        User cashier = saveUser("Grace", "Iradukunda", "cashier@posify.demo", "+250780000003",
                EUserRole.ROLE_BRANCH_CASHIER, tenantId, encoded, now);
        cashier.setStore(store);
        cashier.setBranch(branch);
        userRepository.save(cashier);

        User customer = saveUser("Eric", "Niyonzima", "customer@posify.demo", "+250780000004",
                EUserRole.ROLE_CUSTOMER, tenantId, encoded, now);
        customer.setStore(store);
        userRepository.save(customer);

        Category groceries = categoryRepository.save(Category.builder()
                .name("Groceries").store(store).tenantId(tenantId).build());
        Category beverages = categoryRepository.save(Category.builder()
                .name("Beverages").store(store).tenantId(tenantId).build());
        Category household = categoryRepository.save(Category.builder()
                .name("Household").store(store).tenantId(tenantId).build());

        seedProduct(store, tenantId, groceries, "Inyange Milk 1L", "MILK-1L", 1200, 80, branch);
        seedProduct(store, tenantId, groceries, "Akabanga 125ml", "AKABANGA-125", 1500, 40, branch);
        seedProduct(store, tenantId, beverages, "Coca-Cola 500ml", "COKE-500", 800, 120, branch);
        seedProduct(store, tenantId, beverages, "Rwanda Mountain Tea", "TEA-500", 2500, 35, branch);
        seedProduct(store, tenantId, household, "Savon de Marseille", "SOAP-MAR", 1800, 50, branch);

        log.info("=== Demo seed complete. Logins use password Demo!123 (super admin is admin!123) ===");
    }

    private User saveUser(String first, String last, String email, String phone,
                          EUserRole role, UUID tenantId, String encoded, LocalDateTime now) {
        User user = User.builder()
                .firstName(first)
                .lastName(last)
                .email(email)
                .phone(phone)
                .role(role)
                .password(encoded)
                .userStatus(EUserStatus.ACTIVE)
                .tenantId(tenantId)
                .createdAt(now)
                .updatedAt(now)
                .lastLogin(now)
                .build();
        return userRepository.save(user);
    }

    private void seedProduct(Store store, UUID tenantId, Category category, String name, String sku,
                             double price, int qty, Branch branch) {
        Product product = productRepository.save(Product.builder()
                .name(name)
                .sku(sku)
                .description("Demo product")
                .mrp(price)
                .sellingPrice(price)
                .brand("POSify Demo")
                .category(category)
                .store(store)
                .tenantId(tenantId)
                .build());
        inventoryRepository.save(Inventory.builder()
                .branch(branch)
                .product(product)
                .quantity(qty)
                .build());
    }
}
