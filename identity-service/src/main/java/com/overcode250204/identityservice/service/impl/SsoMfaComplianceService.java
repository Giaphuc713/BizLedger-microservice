package com.overcode250204.identityservice.service.impl;

import com.overcode250204.identityservice.outbox.OutboxEvent;
import com.overcode250204.identityservice.dto.auth.AuthResponse;
import com.overcode250204.identityservice.dto.auth.SsoLoginRequest;
import com.overcode250204.identityservice.entity.*;
import com.overcode250204.identityservice.exception.UserInactiveException;
import com.overcode250204.identityservice.outbox.OutboxEventRepository;
import com.overcode250204.identityservice.repository.*;
import com.overcode250204.identityservice.service.ISsoMfaComplianceService;
import com.overcode250204.identityservice.util.OidcTokenValidator;
import com.overcode250204.identityservice.util.TotpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SsoMfaComplianceService implements ISsoMfaComplianceService {

    private static final List<String> DEFAULT_OWNER_PERMISSIONS = List.of(
            "product.manage",
            "inventory.manage",
            "order.create",
            "order.approve",
            "payment.manage",
            "report.view",
            "user.manage");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtTokenService jwtTokenService;
    private final CdcSubscriptionRepository cdcSubscriptionRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public AuthResponse ssoLogin(SsoLoginRequest request) {
        Map<String, Object> claims = OidcTokenValidator.parseAndValidateToken(request.provider(),
                request.idToken());

        String email = (String) claims.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OIDC ID Token has missing or empty email field");
        }
        email = email.trim().toLowerCase();
        String sub = (String) claims.get("sub");

        String fullName = (String) claims.get("name");
        if (fullName == null || fullName.isBlank()) {
            fullName = (String) claims.get("given_name");
        }
        if (fullName == null || fullName.isBlank()) {
            fullName = email.split("@")[0];
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (!user.isActive()) {
                throw new UserInactiveException(email);
            }
            // Bind SSO subject key if not already bound
            if (user.getSsoProvider() == null || user.getSsoSubject() == null) {
                user.setSsoProvider(request.provider());
                user.setSsoSubject(sub);
                userRepository.save(user);
            }
        } else {
            // User does not exist, auto-onboard a new tenant and user mapping
            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            String tenantName = fullName + "'s Business";
            String rawCode = email.split("@")[0].replaceAll("[^a-zA-Z0-9 ]", "").trim().replace(" ", "_");
            String tenantCode = rawCode.toUpperCase() + "_TENANT";

            // If tenant code exists, append random suffix
            if (tenantRepository.existsByCode(tenantCode)) {
                tenantCode = tenantCode + "_"
                        + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            }

            Tenant tenant = new Tenant(
                    tenantId,
                    tenantName,
                    tenantCode,
                    "ACTIVE",
                    OffsetDateTime.now());
            tenantRepository.save(tenant);

            user = User.builder()
                    .id(userId)
                    .tenantId(tenantId)
                    .email(email)
                    .passwordHash(UUID.randomUUID().toString()) // Random hash since they use SSO
                    .fullName(fullName)
                    .active(true)
                    .createdAt(OffsetDateTime.now())
                    .ssoProvider(request.provider())
                    .ssoSubject(sub)
                    .mfaEnabled(false)
                    .build();
            userRepository.save(user);

            Role ownerRole = new Role(UUID.randomUUID(), tenantId, "OWNER", "Owner");
            roleRepository.save(ownerRole);

            // Assign default permissions
            for (String permissionCode : DEFAULT_OWNER_PERMISSIONS) {
                Permission permission = permissionRepository.findByCode(permissionCode)
                        .orElseGet(() -> permissionRepository.save(
                                new Permission(UUID.randomUUID(), permissionCode,
                                        permissionCode)));
                userRoleRepository.assignPermissionToRole(ownerRole.getId(), permission.getId());
            }

            userRoleRepository.assignRoleToUser(user.getId(), ownerRole.getId());
        }

        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRoleRepository.findPermissionCodesByUserId(user.getId());

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(), user.getTenantId(), user.getEmail(), roles, permissions);

        return new AuthResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getFullName(),
                roles,
                permissions);
    }

    @Override
    public List<Map<String, Object>> getSsoProviders() {
        return List.of(
                Map.of("id", "sso-okta", "name", "Okta", "protocol", "OIDC", "status", "configured"),
                Map.of("id", "sso-azure", "name", "Azure AD", "protocol", "SAML", "status",
                        "not_configured"),
                Map.of("id", "sso-google", "name", "Google Workspace", "protocol", "OIDC", "status",
                        "not_configured"));
    }

    @Override
    @Transactional
    public Map<String, Object> enrollMfa() {
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException(
                        "Authenticated user not found in database: " + principal.userId()));

        String secret = TotpUtil.generateSecretKey();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrUri = "otpauth://totp/BizLedger:" + user.getEmail() + "?secret=" + secret
                + "&issuer=BizLedger";

        return Map.of(
                "secret", secret,
                "qrUri", qrUri);
    }

    @Override
    @Transactional
    public Map<String, Object> verifyMfa(String code) {
        AuthenticatedUser principal = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new RuntimeException(
                        "Authenticated user not found in database: " + principal.userId()));

        if (user.getMfaSecret() == null) {
            throw new BadCredentialsException(
                    "MFA enrollment has not been initialized. Please enroll first.");
        }

        boolean verified = TotpUtil.verifyOtp(user.getMfaSecret(), code);
        if (verified) {
            user.setMfaEnabled(true);
            userRepository.save(user);
        }

        return Map.of("verified", verified);
    }

    private static final Map<String, TokenBucket> RATE_LIMITERS = new java.util.concurrent.ConcurrentHashMap<>();

    private static class TokenBucket {
        private final long capacity = 10000;
        private final double refillRate = 2.77;
        private double tokens = 10000.0;
        private long lastRefillTime = System.currentTimeMillis();

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefillTime) / 1000.0;
            lastRefillTime = now;
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized long getRemaining() {
            long now = System.currentTimeMillis();
            double elapsed = (now - lastRefillTime) / 1000.0;
            double current = Math.min(capacity, tokens + elapsed * refillRate);
            return (long) current;
        }
    }

    @Override
    public Map<String, Object> getRateLimit(String tenantId) {
        TokenBucket bucket = RATE_LIMITERS.computeIfAbsent(tenantId, k -> new TokenBucket());
        bucket.tryConsume();
        return Map.of(
                "tenantId", tenantId,
                "limit", 10000,
                "remaining", bucket.getRemaining(),
                "resetAt", OffsetDateTime.now().plusHours(1).toString());
    }

    @Override
    public Map<String, Object> requestDataExport(String subjectId) {
        User usr = null;
        try {
            UUID userId = UUID.fromString(subjectId);
            usr = userRepository.findById(userId).orElse(null);
        } catch (IllegalArgumentException e) {
            usr = userRepository.findByEmail(subjectId).orElse(null);
        }

        if (usr == null) {
            throw new RuntimeException("Data subject user not found: " + subjectId);
        }

        List<String> roles = userRoleRepository.findRoleCodesByUserId(usr.getId());
        List<String> permissions = userRoleRepository.findPermissionCodesByUserId(usr.getId());
        Tenant tenant = tenantRepository.findById(usr.getTenantId()).orElse(null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("user_id", usr.getId().toString());
        profile.put("email", usr.getEmail());
        profile.put("full_name", usr.getFullName());
        profile.put("sso_provider", usr.getSsoProvider());
        profile.put("sso_subject", usr.getSsoSubject());
        profile.put("mfa_enabled", usr.isMfaEnabled());
        profile.put("created_at", usr.getCreatedAt() != null ? usr.getCreatedAt().toString() : "");
        profile.put("roles", roles);
        profile.put("permissions", permissions);
        if (tenant != null) {
            profile.put("tenant", Map.of(
                    "id", tenant.getId().toString(),
                    "name", tenant.getName(),
                    "code", tenant.getCode(),
                    "status", tenant.getStatus()));
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(profile);
            String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String dataUri = "data:application/json;base64," + base64;

            return Map.of(
                    "jobId", "dsar_" + UUID.randomUUID().toString().substring(0, 8),
                    "downloadUrl", dataUri,
                    "expiresAt", OffsetDateTime.now().plusDays(1).toString(),
                    "exportedData", profile);
        } catch (Exception e) {
            throw new RuntimeException("Error executing data export JSON serialization: " + e.getMessage(),
                    e);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> requestErasure(String subjectId) {
        User usr = null;
        try {
            UUID userId = UUID.fromString(subjectId);
            usr = userRepository.findById(userId).orElse(null);
        } catch (IllegalArgumentException e) {
            usr = userRepository.findByEmail(subjectId).orElse(null);
        }

        if (usr == null) {
            throw new RuntimeException("Data subject user not found: " + subjectId);
        }

        usr.setFullName("GDPR-ANONYMIZED");
        usr.setEmail("anonymized-" + usr.getId().toString() + "@bizledger.io");
        usr.setPasswordHash("DELETED");
        usr.setMfaSecret(null);
        usr.setMfaEnabled(false);
        usr.setSsoSubject(null);
        usr.setSsoProvider(null);
        usr.setActive(false);
        userRepository.save(usr);

        return Map.of(
                "status", "COMPLETED",
                "subjectId", subjectId,
                "anonymizedFields",
                List.of("fullName", "email", "passwordHash", "mfaSecret", "ssoSubject"),
                "retainedFields", List.of("id", "tenantId", "createdAt"),
                "reason", "Financial records retained for audit logs & tax compliance");
    }

    @Override
    public Map<String, Object> exportAuditSiem(String format) {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        StringBuilder ndjsonBuilder = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        for (OutboxEvent event : events) {
            try {
                Map<String, Object> map = new HashMap<>();
                map.put("eventId", event.getEventId());
                map.put("eventType", event.getEventType());
                map.put("topic", event.getTopic());
                map.put("status", event.getStatus());
                map.put("retryCount", event.getRetryCount());
                map.put("createdAt",
                        event.getCreatedAt() != null ? event.getCreatedAt().toString() : "");
                map.put("publishedAt",
                        event.getPublishedAt() != null ? event.getPublishedAt().toString()
                                : "");
                try {
                    map.put("payload", mapper.readValue(event.getPayload(), Map.class));
                } catch (Exception parseEx) {
                    map.put("payload", event.getPayload());
                }
                ndjsonBuilder.append(mapper.writeValueAsString(map)).append("\n");
            } catch (Exception e) {
                // ignore
            }
        }

        String content = ndjsonBuilder.toString();
        String base64 = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        String mimeType = "ndjson".equalsIgnoreCase(format) ? "application/x-ndjson" : "application/json";
        String dataUri = "data:" + mimeType + ";base64," + base64;

        return Map.of(
                "url", dataUri,
                "records", events.size(),
                "format", format);
    }

    @Override
    public List<Map<String, Object>> getCdcSubscriptions() {
        List<CdcSubscription> subs = cdcSubscriptionRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        for (CdcSubscription sub : subs) {
            list.add(Map.of(
                    "id", sub.getId().toString(),
                    "endpoint", sub.getEndpoint(),
                    "tables", sub.getTablesList(),
                    "lastSyncAt", sub.getLastSyncAt() != null ? sub.getLastSyncAt().toString() : "",
                    "status", sub.getStatus()));
        }
        return list;
    }

    @Override
    @Transactional
    public Map<String, Object> createCdcSubscription(Map<String, Object> body) {
        String endpoint = (String) body.getOrDefault("endpoint", "");
        List<String> tables = (List<String>) body.getOrDefault("tables", List.of());

        CdcSubscription sub = new CdcSubscription();
        sub.setId(UUID.randomUUID());
        sub.setEndpoint(endpoint);
        sub.setTablesList(tables);
        sub.setStatus("active");
        sub.setLastSyncAt(OffsetDateTime.now());

        CdcSubscription saved = cdcSubscriptionRepository.save(sub);

        return Map.of(
                "id", saved.getId().toString(),
                "endpoint", saved.getEndpoint(),
                "tables", saved.getTablesList(),
                "lastSyncAt", saved.getLastSyncAt().toString(),
                "status", saved.getStatus());
    }
}
