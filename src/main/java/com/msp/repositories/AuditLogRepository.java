package com.msp.repositories;

import com.msp.enums.EAuditAction;
import com.msp.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(UUID tenantId, EAuditAction action, Pageable pageable);

    Page<AuditLog> findByActorId(UUID actorId, Pageable pageable);
}
