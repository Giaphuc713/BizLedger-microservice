package com.overcode250204.identityservice.service.impl;

import com.overcode250204.common.annotation.AuditLog;
import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.LoginRequest;
import com.overcode250204.identityservice.dto.auth.MeResponse;
import com.overcode250204.identityservice.dto.auth.RegisterTenantRequest;
import com.overcode250204.identityservice.dto.auth.Rotation;
import com.overcode250204.identityservice.entity.*;
import com.overcode250204.identityservice.exception.EmailAlreadyExistsException;
import com.overcode250204.identityservice.exception.TenantCodeExistsException;
import com.overcode250204.identityservice.exception.UserInactiveException;
import com.overcode250204.identityservice.repository.*;
import com.overcode250204.identityservice.service.IAuthService;
import com.overcode250204.identityservice.service.IJwtTokenService;
import com.overcode250204.identityservice.service.IOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final JavaMailSender mailSender;

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
    private final ResetPasswordRepository resetPasswordRepository;
    private final RefreshTokenService refreshTokenService;

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
            userRoleRepository.assignPermissionToRole(ownerRole.getId(), permission.getId());
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
        String refreshToken = refreshTokenService.createRefreshToken(owner);

        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, tenantId,
                ownerEmail, owner.getFullName(), roles, permissions);
    }

    @Override
    @AuditLog(action = "USER_LOGIN", resource = "auth", description = "User authentication attempt")
    @Transactional
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
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, HEADER_TOKEN_TYPE,
                user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), roles, permissions);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = rotation.user();
        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRoleRepository.findPermissionCodesByUserId(user.getId());
        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(), user.getTenantId(), user.getEmail(), roles, permissions);

        return new AuthResponse(
                accessToken,
                rotation.refreshToken(),
                HEADER_TOKEN_TYPE,
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getFullName(),
                roles,
                permissions);
    }

    @Override
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRoleRepository.findPermissionCodesByUserId(user.getId());

        return new MeResponse(user.getId(), user.getTenantId(), user.getEmail(), roles, permissions);
    }


    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        log.info("Password reset requested to email: {}", normalizedEmail);
        // Return the same API response whether or not this address is registered.
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            OffsetDateTime now = OffsetDateTime.now();

            resetPasswordRepository.save(ForgotPassword.builder()
                    .id(UUID.randomUUID())
                    .email(normalizedEmail)
                    .token(sha256(token))
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(RESET_TOKEN_TTL_MINUTES))
                    .build());


            String body = "<p>Use the link below to choose a new password. It expires in 30 minutes.</p>"
                    + "<p>" + token + "</p>";
            sendHtmlMessage("Giaphucha2639@gmail.com", "Reset your BizLedger password", body);
        });
    }


    public void sendHtmlMessage(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText("<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;padding:24px;color:#24324a\">"
                    + "<h2 style=\"color:#3155a4\">BizLedger</h2>" + body + "</div>", true);
            mailSender.send(message);
            log.info("Password reset email sent");
        } catch (MessagingException ex) {
            throw new IllegalStateException("Failed to send email", ex);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        ForgotPassword forgotPassword = resetPasswordRepository.findByToken(sha256(token))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (forgotPassword.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }
        if (forgotPassword.getUsedAt() != null) {
            throw new IllegalArgumentException("Token has already been used");
        }

        User user = userRepository.findByEmail(forgotPassword.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // revoke all refresh tokens for the user after password reset
        refreshTokenService.revokeAllForUser(user);
        forgotPassword.setUsedAt(OffsetDateTime.now());
        resetPasswordRepository.save(forgotPassword);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }


}
