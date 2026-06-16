package com.msp.impls;

import com.msp.enums.EAuditAction;
import com.msp.models.AuditLog;
import com.msp.models.User;
import com.msp.repositories.AuditLogRepository;
import com.msp.services.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepo;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID tenantId, User actor, EAuditAction action,
                    String resourceType, String resourceId,
                    String details, String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .tenantId(tenantId)
                    .actor(actor)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepo.save(entry);
        } catch (Exception e) {
            // Audit failures must never break the primary flow
            log.error("Failed to write audit log: action={}, tenantId={}, error={}",
                    action, tenantId, e.getMessage());
        }
    }

    @Override
    public Page<AuditLog> getLogsForTenant(UUID tenantId, Pageable pageable) {
        return auditLogRepo.findByTenantId(tenantId, pageable);
    }

    @Override
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepo.findAll(pageable);
    }
}
