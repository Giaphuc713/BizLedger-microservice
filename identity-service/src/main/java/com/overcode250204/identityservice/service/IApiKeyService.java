package com.overcode250204.identityservice.service;

import com.overcode250204.identityservice.dto.apikey.ApiKeyDto;
import com.overcode250204.identityservice.dto.apikey.CreateApiKeyRequest;
import com.overcode250204.identityservicejava.dto.apikey.CreateApiKeyResponse;

import java.util.List;
import java.util.UUID;

/**
 * IApiKeyService — Service interface defining administrative API key lifecycle
 * operations,
 * including keys listing, hashing, credentials creation, and revocation.
 */
public interface IApiKeyService {

    /**
     * Lists all API keys associated with the given tenant.
     *
     * @param tenantId unique identifier of the tenant
     * @return collection of API key metadata DTOs
     */
    List<ApiKeyDto> listApiKeys(UUID tenantId);

    /**
     * Generates a new API key for the 指定 tenant based on request configurations.
     *
     * @param tenantId unique identifier of the tenant
     * @param request  creation request specifications
     * @return details of the created key including raw full key text
     */
    CreateApiKeyResponse createApiKey(UUID tenantId, CreateApiKeyRequest request);

    /**
     * Revokes an existing API key, rendering it unusable.
     *
     * @param tenantId unique identifier of the tenant owning the key
     * @param id       unique identifier of the API key to be revoked
     */
    void revokeApiKey(UUID tenantId, UUID id);
}
