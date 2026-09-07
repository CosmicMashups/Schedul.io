package com.clinic.platform.identity.service;

import com.clinic.platform.identity.dto.LoginRequest;
import com.clinic.platform.identity.dto.LoginResponse;
import com.clinic.platform.security.JwtService;
import com.clinic.platform.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long accessTokenTtlMinutes;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                        @Value("${clinic.security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    /**
     * Requires TenantContext to already be resolved (X-Tenant-Id header) — a user can only
     * authenticate against the tenant the request declares, since (tenant_id, email) is the
     * actual unique identity, not email alone.
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);

        List<String> roles = principal.getAuthorities().stream()
                .map(Object::toString)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();

        return new LoginResponse(
                accessToken,
                "Bearer",
                accessTokenTtlMinutes * 60,
                principal.getUserId(),
                principal.getTenantId(),
                principal.getUsername(),
                roles
        );
    }
}
