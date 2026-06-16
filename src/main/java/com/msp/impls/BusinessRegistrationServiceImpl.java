package com.msp.impls;

import com.msp.enums.EAuditAction;
import com.msp.enums.ERegistrationStatus;
import com.msp.exceptions.BusinessRegistrationException;
import com.msp.mappers.TenantRegistrationMapper;
import com.msp.models.TenantRegistration;
import com.msp.models.User;
import com.msp.payloads.dtos.TenantRegistrationDto;
import com.msp.payloads.request.BusinessRegistrationRequest;
import com.msp.payloads.response.ProvisioningResponse;
import com.msp.repositories.TenantRegistrationRepository;
import com.msp.repositories.UserRepository;
import com.msp.services.AuditLogService;
import com.msp.services.BusinessRegistrationService;
import com.msp.services.TenantProvisioningService;
import com.msp.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.msp.enums.EOutboxStatus;
import com.msp.events.RegistrationEventPayload;
import com.msp.models.OutboxEvent;
import com.msp.repositories.OutboxEventRepository;
import com.msp.services.AwsSnsService;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessRegistrationServiceImpl implements BusinessRegistrationService {

    private final TenantRegistrationRepository registrationRepo;
    private final UserRepository userRepo;
    private final TenantProvisioningService provisioningService;
    private final AuditLogService auditLogService;
    private final AwsSnsService snsService;
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.notification-queue-url}")
    private String notificationQueueUrl;

    @Value("${aws.sns.admin-topic-arn}")
    private String adminTopicArn;

    // ── Submit ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TenantRegistrationDto submitRegistration(BusinessRegistrationRequest request) {

        // Uniqueness checks
        if (registrationRepo.existsByOwnerEmail(request.getOwnerEmail())) {
            throw new BusinessRegistrationException(
                    "A registration with email '" + request.getOwnerEmail() + "' already exists.");
        }
        if (userRepo.findByEmail(request.getOwnerEmail()) != null) {
            throw new BusinessRegistrationException(
                    "An account with email '" + request.getOwnerEmail() + "' already exists.");
        }
        if (registrationRepo.existsByBusinessName(request.getBusinessName())) {
            throw new BusinessRegistrationException(
                    "A registration with business name '" + request.getBusinessName() + "' already exists.");
        }

        TenantRegistration reg = TenantRegistration.builder()
                .ownerFirstName(request.getOwnerFirstName())
                .ownerLastName(request.getOwnerLastName())
                .ownerEmail(request.getOwnerEmail())
                .ownerPhone(request.getOwnerPhone())
                .businessName(request.getBusinessName())
                .legalName(request.getLegalName())
                .registrationNumber(request.getRegistrationNumber())
                .country(request.getCountry())
                .industry(request.getIndustry())
                .businessDescription(request.getBusinessDescription())
                .status(ERegistrationStatus.PENDING)
                .build();

        reg = registrationRepo.save(reg);

        log.info("Business registration submitted: id={}, businessName={}, ownerEmail={}",
                reg.getId(), reg.getBusinessName(), reg.getOwnerEmail());

        auditLogService.log(null, null, EAuditAction.REGISTRATION_SUBMITTED,
                "TenantRegistration", reg.getId().toString(),
                "businessName=" + reg.getBusinessName(), null);

        try {
            RegistrationEventPayload emailPayload = RegistrationEventPayload.builder()
                    .eventType("REGISTRATION_SUBMITTED")
                    .registrationId(reg.getId())
                    .ownerEmail(reg.getOwnerEmail())
                    .ownerFirstName(reg.getOwnerFirstName())
                    .businessName(reg.getBusinessName())
                    .build();

            OutboxEvent outbox = OutboxEvent.builder()
                    .eventType("REGISTRATION_SUBMITTED")
                    .queueUrl(notificationQueueUrl)
                    .payload(objectMapper.writeValueAsString(emailPayload))
                    .status(EOutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepo.save(outbox);
        } catch (Exception e) {
            log.error("Failed to queue registration confirmation email to outbox for registration: {}", reg.getId(), e);
        }

        try {
            String adminMsg = String.format("New pending business registration: %s submitted by %s", 
                    reg.getBusinessName(), reg.getOwnerEmail());
            snsService.publishNotification(adminTopicArn, "New MSP Registration", adminMsg);
        } catch (Exception e) {
            log.error("Failed to publish SNS notification for new registration: {}", reg.getId(), e);
        }

        return TenantRegistrationMapper.toDto(reg);
    }

    // ── Read ────────────────────────────────────────────────────────────────

    @Override
    public TenantRegistrationDto getRegistration(UUID registrationId) {
        TenantRegistration reg = findOrThrow(registrationId);
        return TenantRegistrationMapper.toDto(reg);
    }

    @Override
    public Page<TenantRegistrationDto> listRegistrations(ERegistrationStatus status, Pageable pageable) {
        if (status != null) {
            return registrationRepo.findByStatus(status, pageable)
                    .map(TenantRegistrationMapper::toDto);
        }
        return registrationRepo.findAll(pageable)
                .map(TenantRegistrationMapper::toDto);
    }

    // ── Admin decisions ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProvisioningResponse approveRegistration(UUID registrationId, String adminNotes) {
        TenantRegistration reg = findOrThrow(registrationId);
        assertTransitionAllowed(reg, ERegistrationStatus.APPROVED);

        // Resolve the current admin from the security context
        User admin = getCurrentAdminFromContext();

        reg.setStatus(ERegistrationStatus.APPROVED);
        reg.setAdminNotes(adminNotes);
        reg.setReviewedBy(admin);
        reg.setReviewedAt(LocalDateTime.now());
        registrationRepo.save(reg);

        log.info("Registration approved: id={}, by={}", registrationId,
                admin != null ? admin.getEmail() : "system");

        auditLogService.log(null, admin, EAuditAction.REGISTRATION_APPROVED,
                "TenantRegistration", registrationId.toString(),
                "adminNotes=" + adminNotes, null);

        return provisioningService.provisionTenant(registrationId);
    }

    @Override
    @Transactional
    public TenantRegistrationDto rejectRegistration(UUID registrationId, String rejectionReason) {
        TenantRegistration reg = findOrThrow(registrationId);
        assertTransitionAllowed(reg, ERegistrationStatus.REJECTED);

        User admin = getCurrentAdminFromContext();

        reg.setStatus(ERegistrationStatus.REJECTED);
        reg.setRejectionReason(rejectionReason);
        reg.setReviewedBy(admin);
        reg.setReviewedAt(LocalDateTime.now());
        reg = registrationRepo.save(reg);

        log.info("Registration rejected: id={}, reason={}", registrationId, rejectionReason);

        auditLogService.log(null, admin, EAuditAction.REGISTRATION_REJECTED,
                "TenantRegistration", registrationId.toString(),
                "reason=" + rejectionReason, null);

        try {
            RegistrationEventPayload emailPayload = RegistrationEventPayload.builder()
                    .eventType("REGISTRATION_REJECTED")
                    .registrationId(reg.getId())
                    .ownerEmail(reg.getOwnerEmail())
                    .ownerFirstName(reg.getOwnerFirstName())
                    .businessName(reg.getBusinessName())
                    .content(rejectionReason)
                    .build();

            OutboxEvent outbox = OutboxEvent.builder()
                    .eventType("REGISTRATION_REJECTED")
                    .queueUrl(notificationQueueUrl)
                    .payload(objectMapper.writeValueAsString(emailPayload))
                    .status(EOutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxRepo.save(outbox);
        } catch (Exception e) {
            log.error("Failed to queue registration rejection email to outbox for registration: {}", reg.getId(), e);
        }

        return TenantRegistrationMapper.toDto(reg);
    }

    @Override
    @Transactional
    public TenantRegistrationDto markUnderReview(UUID registrationId) {
        TenantRegistration reg = findOrThrow(registrationId);
        assertTransitionAllowed(reg, ERegistrationStatus.UNDER_REVIEW);

        reg.setStatus(ERegistrationStatus.UNDER_REVIEW);
        reg = registrationRepo.save(reg);

        log.info("Registration marked UNDER_REVIEW: id={}", registrationId);
        return TenantRegistrationMapper.toDto(reg);
    }

    @Override
    @Transactional
    public TenantRegistrationDto resubmitRegistration(UUID registrationId,
                                                       BusinessRegistrationRequest updated) {
        TenantRegistration reg = findOrThrow(registrationId);

        if (reg.getStatus() != ERegistrationStatus.REJECTED) {
            throw new BusinessRegistrationException(
                    "Only REJECTED registrations can be resubmitted. Current status: " + reg.getStatus());
        }

        // Update fields from the new request
        reg.setOwnerFirstName(updated.getOwnerFirstName());
        reg.setOwnerLastName(updated.getOwnerLastName());
        reg.setOwnerPhone(updated.getOwnerPhone());
        reg.setBusinessName(updated.getBusinessName());
        reg.setLegalName(updated.getLegalName());
        reg.setRegistrationNumber(updated.getRegistrationNumber());
        reg.setCountry(updated.getCountry());
        reg.setIndustry(updated.getIndustry());
        reg.setBusinessDescription(updated.getBusinessDescription());

        // Reset lifecycle fields
        reg.setStatus(ERegistrationStatus.PENDING);
        reg.setRejectionReason(null);
        reg.setAdminNotes(null);
        reg.setReviewedBy(null);
        reg.setReviewedAt(null);

        reg = registrationRepo.save(reg);

        auditLogService.log(null, null, EAuditAction.REGISTRATION_SUBMITTED,
                "TenantRegistration", reg.getId().toString(),
                "resubmitted; businessName=" + reg.getBusinessName(), null);

        log.info("Registration resubmitted: id={}, businessName={}", reg.getId(), reg.getBusinessName());
        return TenantRegistrationMapper.toDto(reg);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private TenantRegistration findOrThrow(UUID id) {
        return registrationRepo.findById(id)
                .orElseThrow(() -> new BusinessRegistrationException(
                        "Registration not found: " + id));
    }

    /**
     * Validates that the requested state transition is legal.
     * Allowed transitions:
     *   PENDING       → UNDER_REVIEW | APPROVED | REJECTED
     *   UNDER_REVIEW  → APPROVED | REJECTED
     *   APPROVED      → (terminal — no further transitions)
     *   REJECTED      → (terminal — no further transitions)
     */
    private void assertTransitionAllowed(TenantRegistration reg, ERegistrationStatus target) {
        ERegistrationStatus current = reg.getStatus();

        boolean allowed = switch (current) {
            case PENDING      -> target == ERegistrationStatus.UNDER_REVIEW
                                 || target == ERegistrationStatus.APPROVED
                                 || target == ERegistrationStatus.REJECTED;
            case UNDER_REVIEW -> target == ERegistrationStatus.APPROVED
                                 || target == ERegistrationStatus.REJECTED;
            case APPROVED, REJECTED -> false;
        };

        if (!allowed) {
            throw new BusinessRegistrationException(
                    "Cannot transition registration from " + current + " to " + target);
        }
    }

    @Override
    @Transactional
    public void addDocumentKey(UUID registrationId, String s3Key) {
        TenantRegistration reg = findOrThrow(registrationId);
        reg.getDocumentS3Keys().add(s3Key);
        registrationRepo.save(reg);
        log.info("Document key '{}' added to registration '{}'", s3Key, registrationId);
    }

    /**
     * Resolves the currently authenticated admin from the security context.
     * Returns null if called outside a security context (e.g. tests).
     */
    private User getCurrentAdminFromContext() {
        try {
            String email = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
            return userRepo.findByEmail(email);
        } catch (Exception e) {
            return null;
        }
    }
}
