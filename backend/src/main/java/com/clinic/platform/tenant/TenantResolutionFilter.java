package com.clinic.platform.tenant;

import com.clinic.platform.tenant.domain.Tenant;
import com.clinic.platform.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves tenant strictly from the X-Tenant-Id header for now (config key:
 * clinic.tenant.default-tenant-strategy). Runs BEFORE Spring Security's filter chain so that
 * authentication/authorization can also validate the JWT's tenant claim against this value
 * (see {@link com.clinic.platform.security.JwtAuthenticationFilter}).
 *
 * Public/unauthenticated endpoints (health checks, swagger) skip resolution entirely.
 */
@Component
@Order(1)
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantRepository tenantRepository;

    public TenantResolutionFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(TENANT_HEADER);
            if (header != null && !header.isBlank()) {
                UUID tenantId = parseTenantId(header, response);
                if (tenantId == null) {
                    return; // response already written by parseTenantId
                }
                Optional<Tenant> tenant = tenantRepository.findByIdAndStatus(tenantId, Tenant.TenantStatus.ACTIVE);
                if (tenant.isEmpty()) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown or inactive tenant.");
                    return;
                }
                TenantContext.setTenantId(tenant.get().getId());
            }
            // If no header is present, TenantContext stays empty. Public endpoints (auth, doctor
            // directory search) tolerate this; anything tenant-scoped calls TenantContext.requireTenantId()
            // and fails fast with a clear 400 rather than silently leaking cross-tenant data.
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID parseTenantId(String header, HttpServletResponse response) throws IOException {
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "X-Tenant-Id header is not a valid UUID.");
            return null;
        }
    }
}
