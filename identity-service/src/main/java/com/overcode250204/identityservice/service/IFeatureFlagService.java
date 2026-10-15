package com.overcode250204.identityservice.service;

import com.overcode250204.identityservice.dto.featureflag.FeatureFlagDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IFeatureFlagService — Service interface outlining tenant-specific feature
 * toggle rules,
 * status lookups, and dynamic configuration override.
 */
public interface IFeatureFlagService {

    /**
     * Retrieves all feature flags configured for a specific tenant,
     * executing auto-seeding if no flags currently exist.
     *
     * @param tenantId unique identifier of the tenant
     * @return collection of feature flag configuration DTOs
     */
    List<FeatureFlagDto> listFeatureFlags(UUID tenantId);

    /**
     * Modifies the parameters (e.g. rolloutPct, status, tenantOverrides) of a
     * feature flag.
     *
     * @param tenantId unique identifier of the tenant
     * @param key      unique string key identifying the feature flag
     * @param patch    map containing updated values
     * @return the updated feature flag DTO details
     */
    FeatureFlagDto updateFeatureFlag(UUID tenantId, String key, Map<String, Object> patch);
}
