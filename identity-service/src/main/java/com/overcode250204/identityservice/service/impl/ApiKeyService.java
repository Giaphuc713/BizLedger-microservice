package com.overcode250204.identityservice.service.impl;

import com.overcode250204.identityservice.dto.apikey.ApiKeyDto;
import com.overcode250204.identityservice.dto.apikey.CreateApiKeyRequest;
import com.overcode250204.identityservicejava.dto.apikey.CreateApiKeyResponse;
import com.overcode250204.identityservice.entity.ApiKey;
import com.overcode250204.identityservice.repository.ApiKeyRepository;
import com.overcode250204.identityservice.service.IApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ApiKeyServiceImpl — Service implementation detailing core API key management
 * methods,
 * including listing, secure generation, token storage, and revocation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService implements IApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyDto> listApiKeys(UUID tenantId) {
        log.debug("[ApiKeyService] Fetching all API keys for tenant: {}", tenantId);
        return apiKeyRepository.findByTenantId(tenantId).stream()
                .map(key -> new ApiKeyDto(
                        key.getId(),
                        key.getName(),
                        key.getKeyHash().length() > 16 ? key.getKeyHash().substring(0, 16) : key.getKeyHash(),
                        List.of(key.getScopes().split(",")),
                        key.getExpiresAt() == null || key.getExpiresAt().isAfter(OffsetDateTime.now()) ? "active"
                                : "revoked",
                        key.getLastUsedAt(),
                        key.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CreateApiKeyResponse createApiKey(UUID tenantId, CreateApiKeyRequest request) {
        log.info("[ApiKeyService] Creating api key with name '{}' for tenant '{}'", request.name(), tenantId);

        String rawToken = "blk_live_" + UUID.randomUUID().toString().replace("-", "");
        UUID keyId = UUID.randomUUID();

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .tenantId(tenantId)
                .userId(UUID.randomUUID()) // Dummy user ID for service-level token mapping
                .name(request.name().trim())
                .keyHash(rawToken) // Hashed value or raw token stored directly in dev database
                .scopes(String.join(",", request.scopes()))
                .createdAt(OffsetDateTime.now())
                .build();

        apiKeyRepository.save(apiKey);

        return new CreateApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                rawToken.substring(0, 16),
                request.scopes(),
                "active",
                null,
                apiKey.getCreatedAt(),
                rawToken);
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID tenantId, UUID id) {
        log.info("[ApiKeyService] Revoking API key '{}' for tenant '{}'", id, tenantId);
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found"));

        if (!apiKey.getTenantId().equals(tenantId)) {
            log.warn(
                    "[ApiKeyService] Security violation: Tenant '{}' attempted to access key '{}' owned by tenant '{}'",
                    tenantId, id, apiKey.getTenantId());
            throw new IllegalArgumentException("Access denied for tenant");
        }

        // Set expires at past datetime to mark as revoked (or delete it from repo)
        apiKey.setExpiresAt(OffsetDateTime.now().minusDays(1));
        apiKeyRepository.save(apiKey);
    }
}
