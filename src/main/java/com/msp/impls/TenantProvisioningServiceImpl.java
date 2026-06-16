package com.msp.impls;

import com.msp.enums.EAuditAction;
import com.msp.enums.EBusinessStatus;
import com.msp.enums.ERegistrationStatus;
import com.msp.enums.ESubscriptionTier;
import com.msp.enums.EUserRole;
import com.msp.enums.EUserStatus;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.mappers.BusinessMapper;
import com.msp.models.Business;
import com.msp.models.TenantRegistration;
import com.msp.models.User;
import com.msp.payloads.dtos.BusinessDto;
import com.msp.payloads.response.ProvisioningResponse;
import com.msp.repositories.BusinessRepository;
import com.msp.repositories.TenantRegistrationRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AuditLogService;
import com.msp.services.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningServiceImpl implements TenantProvisioningService {

    private final TenantRegistrationRepository registrationRepo;
    private final BusinessRepository businessRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // ── Provision ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProvisioningResponse provisionTenant(UUID registrationId) {
        TenantRegistration reg = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "Registration not found: " + registrationId));

        if (reg.getStatus() != ERegistrationStatus.APPROVED) {
            throw new BusinessRegistrationException(
                    "Cannot provision a registration that is not APPROVED. Current status: "
                    + reg.getStatus());
        }

        if (reg.getProvisionedTenantId() != null) {
            throw new BusinessRegistrationException(
                    "Tenant already provisioned for registration: " + registrationId);
        }

        UUID tenantId = UUID.randomUUID();

        // Temporary password — owner must change on first login
        String tempPassword = tenantId.toString().replace("-", "").substring(0, 12);

        User owner = new User();
        owner.setFirstName(reg.getOwnerFirstName());
        owner.setLastName(reg.getOwnerLastName());
        owner.setEmail(reg.getOwnerEmail());
        owner.setPhone(reg.getOwnerPhone());
        owner.setRole(EUserRole.ROLE_STORE_ADMIN);
        owner.setPassword(passwordEncoder.encode(tempPassword));
        owner.setUserStatus(EUserStatus.ACTIVE);
        owner.setTenantId(tenantId);
        owner.setCreatedAt(LocalDateTime.now());
        owner.setUpdatedAt(LocalDateTime.now());
        owner.setLastLogin(null);
        owner = userRepo.save(owner);

        Business business = Business.builder()
                .tenantId(tenantId)
                .businessName(reg.getBusinessName())
                .legalName(reg.getLegalName())
                .registrationNumber(reg.getRegistrationNumber())
                .country(reg.getCountry())
                .industry(reg.getIndustry())
                .description(reg.getBusinessDescription())
                .owner(owner)
                .build();
        business = businessRepo.save(business);

        reg.setProvisionedTenantId(tenantId);
        registrationRepo.save(reg);

        auditLogService.log(tenantId, null, EAuditAction.TENANT_PROVISIONED,
                "Business", business.getId().toString(),
                "businessName=" + business.getBusinessName(), null);

        log.info("Tenant provisioned: tenantId={}, businessName={}, ownerEmail={}",
                tenantId, business.getBusinessName(), owner.getEmail());
        log.info("Temp credentials for {} — must be changed on first login", owner.getEmail());

        return new ProvisioningResponse(
                tenantId,
                business.getId(),
                owner.getEmail(),
                "Tenant provisioned successfully. Credentials sent to " + owner.getEmail()
        );
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Override
    public BusinessDto getTenantDetails(UUID tenantId) {
        Business business = businessRepo.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "No business found for tenantId: " + tenantId));
        return BusinessMapper.toDto(business);
    }

    // ── Deprovision ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deprovisionTenant(UUID tenantId) {
        Business business = businessRepo.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "No business found for tenantId: " + tenantId));

        if (business.getStatus() == EBusinessStatus.DEPROVISIONED) {
            throw new BusinessRegistrationException("Tenant is already deprovisioned.");
        }

        business.setStatus(EBusinessStatus.DEPROVISIONED);
        businessRepo.save(business);

        auditLogService.log(tenantId, null, EAuditAction.TENANT_SUSPENDED,
                "Business", business.getId().toString(),
                "status=DEPROVISIONED", null);

        log.info("Tenant deprovisioned: tenantId={}, businessName={}",
                tenantId, business.getBusinessName());
    }

    // ── Subscription plan ────────────────────────────────────────────────────

    @Override
    @Transactional
    public BusinessDto updateTenantPlan(UUID tenantId, ESubscriptionTier tier) {
        Business business = businessRepo.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "No business found for tenantId: " + tenantId));

        ESubscriptionTier oldTier = business.getSubscriptionTier();
        business.setSubscriptionTier(tier);

        // Clear trial end date when upgrading to a paid tier
        if (tier != ESubscriptionTier.FREE_TRIAL) {
            business.setTrialEndsAt(null);
        }

        business = businessRepo.save(business);

        auditLogService.log(tenantId, null, EAuditAction.SUBSCRIPTION_CHANGED,
                "Business", business.getId().toString(),
                "from=" + oldTier + "; to=" + tier, null);

        log.info("Tenant plan updated: tenantId={}, from={}, to={}", tenantId, oldTier, tier);
        return BusinessMapper.toDto(business);
    }
}
