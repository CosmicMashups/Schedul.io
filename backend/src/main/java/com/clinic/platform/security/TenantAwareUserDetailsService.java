package com.clinic.platform.security;

import com.clinic.platform.identity.repository.UserRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Users are unique per (tenant_id, email) — the same email can exist in two different clinics'
 * tenants as unrelated accounts. Login therefore always requires the X-Tenant-Id header to be
 * resolved first by {@link com.clinic.platform.tenant.TenantResolutionFilter}.
 */
@Service
public class TenantAwareUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public TenantAwareUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var tenantId = TenantContext.requireTenantId();
        var user = userRepository.findByTenantIdAndEmail(tenantId, email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for this tenant/email combination."));
        return new UserPrincipal(user);
    }
}
