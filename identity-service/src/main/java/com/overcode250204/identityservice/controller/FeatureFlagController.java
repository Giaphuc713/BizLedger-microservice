package com.overcode250204.identityservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.identityservice.dto.featureflag.FeatureFlagDto;
import com.overcode250204.identityservice.service.IFeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FeatureFlagController — Exposes REST endpoints to query and manage dynamic
 * runtime feature flags.
 */
@RestController
@RequestMapping("/api/v1/feature-flags")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final IFeatureFlagService featureFlagService;

    @GetMapping
    @PreAuthorize("hasAuthority('featureflag:read')")
    public ResponseEntity<ApiResponse<List<FeatureFlagDto>>> listFeatureFlags(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.listFeatureFlags(tenantId)));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('featureflag:write')")
    public ResponseEntity<ApiResponse<FeatureFlagDto>> updateFeatureFlag(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable String key,
            @RequestBody Map<String, Object> patch) {
        FeatureFlagDto response = featureFlagService.updateFeatureFlag(tenantId, key, patch);
        return ResponseEntity.ok(ApiResponse.success("Feature flag updated successfully", response));
    }
}
