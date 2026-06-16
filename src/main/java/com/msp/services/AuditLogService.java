package com.msp.services;

import com.msp.enums.EAuditAction;
import com.msp.models.AuditLog;
import com.msp.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {

    /**
     * Writes an audit log entry. Called explicitly at key decision points —
     * not via AOP — so audit intent is always visible in the code.
     *
     * @param tenantId     The tenant this event belongs to (null for system-level events)
     * @param actor        The user who performed the action (null for system actions)
     * @param action       The type of action
     * @param resourceType e.g. "Store", "Order", "TenantRegistration"
     * @param resourceId   The UUID of the affected resource as a string
     * @param details      Optional JSON blob with before/after state or context
     * @param ipAddress    The caller's IP address (may be null)
     */
    void log(UUID tenantId, User actor, EAuditAction action,
             String resourceType, String resourceId, String details, String ipAddress);

    /** Returns audit logs for a specific tenant, newest first. */
    Page<AuditLog> getLogsForTenant(UUID tenantId, Pageable pageable);

    /** Returns all system-wide audit logs. SUPER_ADMIN only. */
    Page<AuditLog> getAllLogs(Pageable pageable);
}
