package com.clinic.platform.practitioner.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkUserRequest(@NotNull UUID userId) {
}
