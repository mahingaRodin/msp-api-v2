package com.msp.impls;

import com.msp.enums.EAuditAction;
import com.msp.enums.EConsentStatus;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.models.AdminConsentRequest;
import com.msp.models.Business;
import com.msp.models.User;
import com.msp.payloads.dtos.AdminConsentRequestDto;
import com.msp.repositories.AdminConsentRequestRepository;
import com.msp.repositories.BusinessRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AdminConsentService;
import com.msp.services.AuditLogService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.msp.enums.EOutboxStatus;
import com.msp.events.ConsentEventPayload;
import com.msp.models.OutboxEvent;
import com.msp.repositories.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminConsentServiceImpl implements AdminConsentService {

    private final AdminConsentRequestRepository consentRepo;
    private final BusinessRepository businessRepo;
    private final UserRepository userRepo;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.notification-queue-url}")
    private String notificationQueueUrl;

    @Override
    @Transactional
    public AdminConsentRequestDto requestConsent(UUID tenantId, String reason, int durationHours) {
        if (durationHours < 1 || durationHours > 72) {
            throw new BusinessRegistrationException("durationHours must be between 1 and 72.");
        }

        businessRepo.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "No active business found for tenantId: " + tenantId));

        User admin = userService.getCurrentUser();

        AdminConsentRequest request = AdminConsentRequest.builder()
                .tenantId(tenantId)
                .requestingAdmin(admin)
                .reason(reason)
                .requestedDurationHours(durationHours)
                .status(EConsentStatus.PENDING)
                .build();

        request = consentRepo.save(request);

        auditLogService.log(tenantId, admin, EAuditAction.CONSENT_REQUESTED,
                "AdminConsentRequest", request.getId().toString(),
                "reason=" + reason + "; durationHours=" + durationHours, null);

        log.info("Consent requested: id={}, tenantId={}, admin={}", request.getId(), tenantId, admin.getEmail());
        try {
            Business business = businessRepo.findByTenantId(tenantId)
                    .orElseThrow(() -> new BusinessRegistrationException("No business found for tenant: " + tenantId));
            User owner = business.getOwner();

            ConsentEventPayload consentPayload = ConsentEventPayload.builder()
                    .consentRequestId(request.getId())
                    .tenantId(tenantId)
                    .reason(reason)
                    .durationHours(durationHours)
                    .adminEmail(admin.getEmail())
                    .ownerEmail(owner.getEmail())
                    .build();

            OutboxEvent outbox = OutboxEvent.builder()
                    .eventType("CONSENT_REQUESTED")
                    .queueUrl(notificationQueueUrl)
                    .payload(objectMapper.writeValueAsString(consentPayload))
                    .status(EOutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepo.save(outbox);
        } catch (Exception e) {
            log.error("Failed to queue consent request email to outbox for request: {}", request.getId(), e);
        }
        return toDto(request);
    }

    @Override
    @Transactional
    public AdminConsentRequestDto grantConsent(UUID consentRequestId, UUID tenantOwnerId) {
        AdminConsentRequest request = findOrThrow(consentRequestId);

        if (request.getStatus() != EConsentStatus.PENDING) {
            throw new BusinessRegistrationException(
                    "Cannot grant a consent request that is not PENDING. Status: " + request.getStatus());
        }

        User owner = userRepo.findById(tenantOwnerId)
                .orElseThrow(() -> new BusinessRegistrationException("User not found: " + tenantOwnerId));

        // Verify the granting user is actually the tenant owner
        if (!tenantOwnerId.equals(request.getTenantId()) &&
                !owner.getTenantId().equals(request.getTenantId())) {
            throw new BusinessRegistrationException("You are not the owner of this tenant.");
        }

        request.setStatus(EConsentStatus.ACTIVE);
        request.setGrantedAt(LocalDateTime.now());
        request.setExpiresAt(LocalDateTime.now().plusHours(request.getRequestedDurationHours()));
        request = consentRepo.save(request);

        auditLogService.log(request.getTenantId(), owner, EAuditAction.CONSENT_GRANTED,
                "AdminConsentRequest", consentRequestId.toString(),
                "expiresAt=" + request.getExpiresAt(), null);

        log.info("Consent granted: id={}, tenantId={}, expiresAt={}",
                consentRequestId, request.getTenantId(), request.getExpiresAt());
        return toDto(request);
    }

    @Override
    @Transactional
    public AdminConsentRequestDto revokeConsent(UUID consentRequestId, UUID revokedById) {
        AdminConsentRequest request = findOrThrow(consentRequestId);

        if (request.getStatus() != EConsentStatus.ACTIVE) {
            throw new BusinessRegistrationException(
                    "Cannot revoke a consent request that is not ACTIVE. Status: " + request.getStatus());
        }

        User revoker = userRepo.findById(revokedById)
                .orElseThrow(() -> new BusinessRegistrationException("User not found: " + revokedById));

        request.setStatus(EConsentStatus.REVOKED);
        request.setRevokedAt(LocalDateTime.now());
        request.setRevokedBy(revoker);
        request = consentRepo.save(request);

        auditLogService.log(request.getTenantId(), revoker, EAuditAction.CONSENT_REVOKED,
                "AdminConsentRequest", consentRequestId.toString(), null, null);

        log.info("Consent revoked: id={}, revokedBy={}", consentRequestId, revoker.getEmail());
        return toDto(request);
    }

    @Override
    public boolean hasActiveConsent(UUID tenantId, UUID adminUserId) {
        return consentRepo.findActiveConsent(tenantId, adminUserId, LocalDateTime.now()).isPresent();
    }

    @Override
    public Page<AdminConsentRequestDto> listConsentRequests(UUID tenantId, Pageable pageable) {
        return consentRepo.findByTenantId(tenantId, pageable).map(this::toDto);
    }

    @Override
    public Page<AdminConsentRequestDto> listMyConsentRequests(UUID adminId, Pageable pageable) {
        return consentRepo.findByRequestingAdminId(adminId, pageable).map(this::toDto);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AdminConsentRequest findOrThrow(UUID id) {
        return consentRepo.findById(id)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "Consent request not found: " + id));
    }

    private AdminConsentRequestDto toDto(AdminConsentRequest r) {
        AdminConsentRequestDto dto = new AdminConsentRequestDto();
        dto.setId(r.getId());
        dto.setTenantId(r.getTenantId());
        dto.setReason(r.getReason());
        dto.setRequestedDurationHours(r.getRequestedDurationHours());
        dto.setStatus(r.getStatus());
        dto.setRequestedAt(r.getRequestedAt());
        dto.setGrantedAt(r.getGrantedAt());
        dto.setExpiresAt(r.getExpiresAt());
        dto.setRevokedAt(r.getRevokedAt());
        if (r.getRequestingAdmin() != null) {
            dto.setRequestingAdminId(r.getRequestingAdmin().getId());
            dto.setRequestingAdminEmail(r.getRequestingAdmin().getEmail());
        }
        if (r.getRevokedBy() != null) {
            dto.setRevokedById(r.getRevokedBy().getId());
        }
        return dto;
    }
}
