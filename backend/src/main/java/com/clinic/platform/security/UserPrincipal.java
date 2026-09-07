package com.clinic.platform.security;

import com.clinic.platform.identity.domain.Role;
import com.clinic.platform.identity.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.userId = user.getId();
        this.tenantId = user.getTenantId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.getStatus() == User.UserStatus.ACTIVE;

        // Roles are exposed as ROLE_<code> (Spring convention), permissions as raw codes
        // (e.g. APPOINTMENT_CREATE) so @PreAuthorize can check either granularity.
        this.authorities = Stream.concat(
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode())),
                user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(p -> new SimpleGrantedAuthority(p.getCode()))
        ).distinct().collect(Collectors.toList());
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
