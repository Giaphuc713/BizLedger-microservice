package com.overcode250204.identityservice.dto.featureflag;

import java.util.List;

/**
 * FeatureFlagDto — Data transfer object representing feature flag
 * configuration,
 * including rollout rate, status, and tenant overrides.
 */
public record FeatureFlagDto(
        String key,
        String description,
        int rolloutPct,
        String status,
        List<String> tenantOverrides) {
}
