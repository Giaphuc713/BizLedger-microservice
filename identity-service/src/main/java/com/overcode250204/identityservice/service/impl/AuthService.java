package com.overcode250204.identityservice.service.impl;

import com.overcode250204.common.annotation.AuditLog;
import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.LoginRequest;
import com.overcode250204.identityservice.dto.auth.MeResponse;
import com.overcode250204.identityservice.dto.auth.RegisterTenantRequest;
import com.overcode250204.identityservice.entity.*;
import com.overcode250204.identityservice.exception.EmailAlreadyExistsException;
import com.overcode250204.identityservice.exception.TenantCodeExistsException;
import com.overcode250204.identityservice.exception.UserInactiveException;
import com.overcode250204.identityservice.repository.*;
import com.overcode250204.identityservice.service.IAuthService;
import com.overcode250204.identityservice.service.IJwtTokenService;
import com.overcode250204.identityservice.service.IOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private static final List<String> DEFAULT_OWNER_PERMISSIONS = List.of(
            "product.manage",
            "inventory.manage",
            "order.create",
            "order.approve",
            "payment.manage",
            "report.view",
            "user.manage");
    private final static String HEADER_TOKEN_TYPE = "Bearer";

    private final IJwtTokenService jwtTokenService;
    private final IOutboxService outboxService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    @AuditLog(action = "TENANT_REGISTERED", resource = "auth", description = "New tenant and owner account created")
    @Override
    public AuthResponse registerTenant(RegisterTenantRequest request) {
        String tenantName = request.tenantName().trim();
        String rawCode = request.tenantCode() != null && !request.tenantCode().trim().isEmpty()
                ? request.tenantCode() : tenantName.replaceAll("[^a-zA-Z0-9 ]", "").trim().replace(" ", "_");
        String tenantCode = normalizeCode(rawCode);
        String ownerEmail = request.ownerEmail().trim().toLowerCase();

        if (tenantRepository.existsByCode(tenantCode)) {
            throw new TenantCodeExistsException(tenantCode);
        }

        if (userRepository.existsByEmail(ownerEmail)) {
            throw new EmailAlreadyExistsException(ownerEmail);
        }

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Tenant tenant = new Tenant(tenantId,
                tenantName,
                tenantCode,
                "ACTIVE",
                OffsetDateTime.now());
        tenantRepository.save(tenant);

        String ownerFullName = request.ownerFullName() != null && !request.ownerFullName().trim().isEmpty()
                ? request.ownerFullName().trim()
                : ownerEmail.split("@")[0];

        User owner = new User(
                userId,
                tenantId,
                ownerEmail,
                passwordEncoder.encode(request.ownerPassword()),
                ownerFullName,
                true,
                OffsetDateTime.now());
        userRepository.save(owner);

        Role ownerRole = new Role(UUID.randomUUID(), tenantId, "OWNER", "Owner");
        roleRepository.save(ownerRole);

        for (String permissionCode : DEFAULT_OWNER_PERMISSIONS) {
            Permission permission = permissionRepository.findByCode(permissionCode)
                    .orElseGet(() -> permissionRepository.save(new Permission(UUID.randomUUID(), permissionCode, permissionCode)));
            userRoleRepository.assignPermissionToRole(owner.getId(), permission.getId());
        }
        userRoleRepository.assignRoleToUser(owner.getId(), ownerRole.getId());

        List<String> roles = List.of("OWNER");
        List<String> permissions = DEFAULT_OWNER_PERMISSIONS;

        outboxService.saveEvent(
                "identity.events",
                "tenant.created",
                tenantId,
                null,
                new LinkedHashMap<>(Map.of(
                        "tenantId", tenantId.toString(),
                        "tenantCode", tenantCode,
                        "tenantName", tenant.getName())
                ));

        outboxService.saveEvent(
                "identity.events",
                "user.created",
                tenantId,
                null,
                new LinkedHashMap<>(Map.of(
                        "userId", userId.toString(),
                        "tenantId", tenantId.toString(),
                        "email", ownerEmail,
                        "fullName", owner.getFullName())));
        String accessToken = jwtTokenService.generateAccessToken(
                userId, tenantId, ownerEmail, roles, permissions);

        return new AuthResponse(accessToken, "Bearer", userId, tenantId,
                ownerEmail, owner.getFullName(), roles, permissions);
    }

    @Override
    @AuditLog(action = "USER_LOGIN", resource = "auth", description = "User authentication attempt")
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UserInactiveException(email);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRoleRepository.findPermissionCodesByUserId(user.getId());

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getTenantId(), user.getEmail(), roles, permissions);

        return new AuthResponse(accessToken, HEADER_TOKEN_TYPE,
                user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), roles, permissions);


    }

    @Override
    public MeResponse me(AuthenticatedUser user) {
        return new MeResponse(user.userId(), user.tenantId(), user.email(), user.roles(), user.permissions());
    }
    private String normalizeCode(String code) {
        return code.trim().toUpperCase().replace(" ", "_");
    }

}
