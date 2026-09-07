package com.clinic.platform.identity.service;

import com.clinic.platform.audit.service.DomainAuditEvent;
import com.clinic.platform.common.exception.ApiException;
import com.clinic.platform.common.util.AuditSnapshot;
import com.clinic.platform.identity.domain.Role;
import com.clinic.platform.identity.domain.User;
import com.clinic.platform.identity.dto.CreateUserRequest;
import com.clinic.platform.identity.dto.UserResponse;
import com.clinic.platform.identity.repository.RoleRepository;
import com.clinic.platform.identity.repository.UserRepository;
import com.clinic.platform.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Closes a gap flagged during the Doctor Portal build: there was no way to create a login
 * account for a doctor (or any staff member) — only the Flyway dev seed created users. This
 * is the general-purpose staff-facing equivalent; the Doctor Portal "invite" flow
 * (staff-portal's Practitioners.tsx) is one caller of it, not a special case.
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final AuditSnapshot auditSnapshot;

    public UserManagementService(UserRepository userRepository, RoleRepository roleRepository,
                                  PasswordEncoder passwordEncoder, ApplicationEventPublisher events,
                                  AuditSnapshot auditSnapshot) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.auditSnapshot = auditSnapshot;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        userRepository.findByTenantIdAndEmail(tenantId, request.email()).ifPresent(u -> {
            throw ApiException.conflict("USER_EXISTS", "A user with this email already exists for this tenant.");
        });

        Set<Role> roles = new HashSet<>();
        for (String code : request.roleCodes()) {
            Role role = roleRepository.findByTenantIdAndCode(tenantId, code)
                    .orElseThrow(() -> ApiException.badRequest("INVALID_ROLE", "Unknown role code: " + code));
            roles.add(role);
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setMobileNumber(request.mobileNumber());
        user.setRoles(roles);

        User saved = userRepository.save(user);

        events.publishEvent(new DomainAuditEvent(
                "USER_CREATE", "User", saved.getId().toString(), null, auditSnapshot.of(saved),
                "roles=" + request.roleCodes()));

        return UserResponse.from(saved);
    }

    /** Used by the staff console to list existing staff accounts a role could be granted to (e.g. picking who to link as a doctor). */
    public List<UserResponse> listByRole(String roleCode) {
        UUID tenantId = TenantContext.requireTenantId();
        return userRepository.findByTenantId(tenantId).stream()
                .filter(u -> roleCode == null || u.getRoles().stream().anyMatch(r -> r.getCode().equals(roleCode)))
                .map(UserResponse::from)
                .toList();
    }
}
