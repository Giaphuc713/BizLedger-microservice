package com.overcode250204.identityservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.identityservice.annotation.CurrentUser;
import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.LoginRequest;
import com.overcode250204.identityservice.dto.auth.MeResponse;
import com.overcode250204.identityservice.dto.auth.RegisterTenantRequest;
import com.overcode250204.identityservice.entity.AuthenticatedUser;
import com.overcode250204.identityservice.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register-tenant")
    public ApiResponse<AuthResponse> registerTenant(
            @Valid @RequestBody RegisterTenantRequest request
    ) {
        return ApiResponse.success(authService.registerTenant(request));
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@CurrentUser AuthenticatedUser user) {
        return ApiResponse.success(authService.me(user));
    }

}
