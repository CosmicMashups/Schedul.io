package com.clinic.platform.tenant;

import java.util.UUID;

/**
 * Holds the resolved tenant (and optionally clinic) for the current request thread.
 * Populated by {@link TenantResolutionFilter} before the request reaches any controller,
 * and read by {@link TenantEntityListener} / repository-level filters so tenant isolation
 * is enforced centrally rather than per-query.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_CLINIC = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setClinicId(UUID clinicId) {
        CURRENT_CLINIC.set(clinicId);
    }

    public static UUID getClinicId() {
        return CURRENT_CLINIC.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new TenantContextMissingException();
        }
        return tenantId;
    }

    /** Must be called at the end of every request (see {@link TenantResolutionFilter}) to avoid thread-pool leakage. */
    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_CLINIC.remove();
    }
}
