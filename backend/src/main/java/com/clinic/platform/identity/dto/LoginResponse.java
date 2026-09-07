package com.clinic.platform.identity.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        UUID tenantId,
        String email,
        List<String> roles
) {
}
