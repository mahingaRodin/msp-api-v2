package com.msp.configs;

import java.util.UUID;

/**
 * Thread-local holder for the current request's tenantId.
 * Populated by TenantContextFilter after JWT validation.
 * Always cleared in a finally block — never leaks between requests.
 */
public class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
