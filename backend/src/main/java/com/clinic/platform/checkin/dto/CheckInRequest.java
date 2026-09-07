package com.clinic.platform.checkin.dto;

import com.clinic.platform.checkin.domain.CheckIn;
import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull CheckIn.CheckInMethod method,
        boolean identityVerified
) {
}
