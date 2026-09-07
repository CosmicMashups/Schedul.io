package com.clinic.platform.identity.dto;

import com.clinic.platform.identity.domain.User;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id, String email, String firstName, String lastName, String status, List<String> roles
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getStatus().name(),
                u.getRoles().stream().map(r -> r.getCode()).toList());
    }
}
