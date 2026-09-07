package com.clinic.platform.identity.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.identity.dto.LoginRequest;
import com.clinic.platform.identity.dto.LoginResponse;
import com.clinic.platform.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint (see clinic.security.public-paths). Requires the X-Tenant-Id header to be
 * set on every call, same as every other endpoint — auth is not exempt from tenant scoping.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
