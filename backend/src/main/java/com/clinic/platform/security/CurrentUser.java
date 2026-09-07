package com.clinic.platform.security;

import com.clinic.platform.common.exception.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<UUID> id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user.userId());
    }

    public static UUID require() {
        return id().orElseThrow(() -> ApiException.unauthorized("AUTH_REQUIRED", "This action requires an authenticated user."));
    }
}
