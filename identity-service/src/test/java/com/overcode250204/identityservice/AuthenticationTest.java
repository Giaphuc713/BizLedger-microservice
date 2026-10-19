package com.overcode250204.identityservice;

import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.RegisterTenantRequest;
import com.overcode250204.identityservice.entity.Permission;
import com.overcode250204.identityservice.entity.Role;
import com.overcode250204.identityservice.entity.Tenant;
import com.overcode250204.identityservice.entity.User;
import com.overcode250204.identityservice.repository.PermissionRepository;
import com.overcode250204.identityservice.repository.RoleRepository;
import com.overcode250204.identityservice.repository.TenantRepository;
import com.overcode250204.identityservice.repository.UserRepository;
import com.overcode250204.identityservice.repository.UserRoleRepository;
import com.overcode250204.identityservice.service.IJwtTokenService;
import com.overcode250204.identityservice.service.IOutboxService;
import com.overcode250204.identityservice.service.impl.AuthService;
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

    @InjectMocks private AuthService authService;

    @Test
    void registerTenant_returnsAuthResponse_whenInputIsValid() {
        RegisterTenantRequest request = new RegisterTenantRequest(
                "AcmeShop", null, "owner@example.com", "plain-pass", null);

        when(tenantRepository.existsByCode("ACMESHOP")).thenReturn(false);
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");

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
        assertEquals("Bearer", response.tokenType());
    }
}
