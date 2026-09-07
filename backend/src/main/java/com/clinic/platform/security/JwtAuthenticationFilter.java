package com.clinic.platform.security;

import com.clinic.platform.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs after {@link com.clinic.platform.tenant.TenantResolutionFilter}. Validates the Bearer
 * token and, critically, rejects the request if the token's tenant claim does not match the
 * tenant already resolved from X-Tenant-Id — this is the second half of tenant isolation
 * (the first half is TenantEntityListener stamping writes).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(header.substring(7));
            UUID tokenTenantId = jwtService.extractTenantId(claims);
            UUID contextTenantId = TenantContext.getTenantId();

            if (contextTenantId != null && !contextTenantId.equals(tokenTenantId)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token does not belong to the requested tenant.");
                return;
            }
            // Token is authoritative even if no X-Tenant-Id header was sent.
            TenantContext.setTenantId(tokenTenantId);

            List<GrantedAuthority> authorities = jwtService.extractAuthorities(claims).stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            AuthenticatedUser principal = new AuthenticatedUser(jwtService.extractUserId(claims), claims.getSubject());
            var authToken = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
