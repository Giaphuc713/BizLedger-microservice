package com.overcode250204.identityservice;

import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.LoginRequest;
import com.overcode250204.identityservice.dto.auth.RegisterTenantRequest;
import com.overcode250204.identityservice.entity.*;

import com.overcode250204.identityservice.repository.*;
import com.overcode250204.identityservice.service.IJwtTokenService;
import com.overcode250204.identityservice.service.IOutboxService;
import com.overcode250204.identityservice.service.impl.AuthService;
import com.overcode250204.identityservice.service.impl.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationTest {

    private static final List<String> EXPECTED_PERMISSIONS = List.of(
            "product.manage", "inventory.manage", "order.create", "order.approve",
            "payment.manage", "report.view", "user.manage");

    @Mock private IJwtTokenService jwtTokenService;
    @Mock private IOutboxService outboxService;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    @Test
    void registerTenant_returnsAuthResponse_whenInputIsValid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "AcmeShop", null, "owner@example.com", "plain-pass", null);

        when(tenantRepository.existsByCode("ACMESHOP")).thenReturn(false);
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("fake-refresh-token");

        for (String permissionCode : EXPECTED_PERMISSIONS) {
            Permission permission = new Permission(UUID.randomUUID(), permissionCode, permissionCode);
            when(permissionRepository.findByCode(permissionCode)).thenReturn(Optional.of(permission));
        }

        when(jwtTokenService.generateAccessToken(
                any(UUID.class), any(UUID.class), eq("owner@example.com"),
                eq(List.of("OWNER")), eq(EXPECTED_PERMISSIONS)))
                .thenReturn("fake-jwt");

        AuthResponse response = authService.registerTenant(request);

        assertEquals("fake-jwt", response.accessToken());
        assertEquals("fake-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void login_returnsAccessAndRefreshTokens_whenCredentialsAreValid() {
        User user = new User(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@example.com",
                "encoded-password",
                "Example User",
                true,
                java.time.OffsetDateTime.now());
        List<String> roles = List.of("OWNER");
        List<String> permissions = List.of("user.manage");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain-password", "encoded-password")).thenReturn(true);
        when(userRoleRepository.findRoleCodesByUserId(user.getId())).thenReturn(roles);
        when(userRoleRepository.findPermissionCodesByUserId(user.getId())).thenReturn(permissions);
        when(jwtTokenService.generateAccessToken(
                user.getId(), user.getTenantId(), user.getEmail(), roles, permissions))
                .thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "plain-password"));

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
    }
}
