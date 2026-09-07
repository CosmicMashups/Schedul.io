package com.clinic.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues and validates access tokens. Every access token embeds the tenant_id claim so that
 * {@link JwtAuthenticationFilter} can cross-check it against the X-Tenant-Id header resolved
 * by {@link com.clinic.platform.tenant.TenantResolutionFilter} — a stolen token cannot be
 * replayed against a different tenant even if the header is spoofed.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlMinutes;

    public JwtService(
            @Value("${clinic.security.jwt.secret}") String secret,
            @Value("${clinic.security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getUserId().toString())
                .claim("tid", principal.getTenantId().toString())
                .claim("authorities", principal.getAuthorities().stream()
                        .map(Object::toString).collect(Collectors.toList()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
    }

    public UUID extractTenantId(Claims claims) {
        return UUID.fromString(claims.get("tid", String.class));
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.get("uid", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(Claims claims) {
        return claims.get("authorities", List.class);
    }
}
