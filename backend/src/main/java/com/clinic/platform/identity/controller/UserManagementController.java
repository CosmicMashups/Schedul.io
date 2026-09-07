package com.clinic.platform.identity.controller;

import com.clinic.platform.common.response.ApiResponse;
import com.clinic.platform.identity.dto.CreateUserRequest;
import com.clinic.platform.identity.dto.UserResponse;
import com.clinic.platform.identity.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userManagementService.create(request));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(@RequestParam(required = false) String role) {
        return ApiResponse.ok(userManagementService.listByRole(role));
    }
}
