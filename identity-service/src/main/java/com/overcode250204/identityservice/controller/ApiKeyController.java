package com.overcode250204.identityservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.identityservice.dto.apikey.ApiKeyDto;
import com.overcode250204.identityservice.dto.apikey.CreateApiKeyRequest;
import com.overcode250204.identityservicejava.dto.apikey.CreateApiKeyResponse;
import com.overcode250204.identityservice.service.IApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * ApiKeyController — Exposes REST endpoints to list, generate, and revoke
 * tenant API Keys.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final IApiKeyService apiKeyService;

    @GetMapping
    @PreAuthorize("hasAuthority('apikey:read')")
    public ResponseEntity<ApiResponse<List<ApiKeyDto>>> listApiKeys(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.listApiKeys(tenantId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('apikey:write')")
    public ResponseEntity<ApiResponse<CreateApiKeyResponse>> createApiKey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody CreateApiKeyRequest request) {
        CreateApiKeyResponse response = apiKeyService.createApiKey(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success("API key created successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('apikey:write')")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID id) {
        apiKeyService.revokeApiKey(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success("API Key successfully revoked", null));
    }
}
