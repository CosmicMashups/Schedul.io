package com.clinic.platform.security;

import java.util.UUID;

/**
 * Principal object set on the SecurityContext by {@link JwtAuthenticationFilter}. Controllers
 * needing the current user's identity (e.g. slot-hold ownership, "createdBy" beyond the
 * auditor string) should use {@link CurrentUser#require()} rather than reading
 * Authentication.getName() directly, since getName() only exposes the email.
 */
public record AuthenticatedUser(UUID userId, String email) {

    @Override
    public String toString() {
        return email; // keeps Authentication.getName() / the JPA auditor field human-readable
    }
}
