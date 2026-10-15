package com.overcode250204.identityservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overcode250204.identityservice.dto.featureflag.FeatureFlagDto;
import com.overcode250204.identityservice.entity.FeatureFlag;
import com.overcode250204.identityservice.repository.FeatureFlagRepository;
import com.overcode250204.identityservice.service.IFeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FeatureFlagServiceImpl — Service implementation managing dynamic feature flag
 * rollout percentage,
 * activation status rules, and seeding standard default toggles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService implements IFeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<FeatureFlagDto> listFeatureFlags(UUID tenantId) {
        log.debug("[FeatureFlagService] Listing feature flags for tenant: {}", tenantId);
        List<FeatureFlagDto> dtos = featureFlagRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        if (dtos.isEmpty()) {
            log.info("[FeatureFlagService] No feature flags found for tenant '{}'. Seeding default toggles.", tenantId);
            seedDefaultFlags(tenantId);
            dtos = featureFlagRepository.findByTenantId(tenantId).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return dtos;
    }

    @Override
    @Transactional
    public FeatureFlagDto updateFeatureFlag(UUID tenantId, String key, Map<String, Object> patch) {
        log.info("[FeatureFlagService] Patching feature flag '{}' for tenant '{}'", key, tenantId);
        FeatureFlag flag = featureFlagRepository.findByTenantIdAndKey(tenantId, key)
                .orElseThrow(() -> new IllegalArgumentException("Feature flag not found"));

        try {
            Map<String, Object> rulesMap = flag.getRules() == null ? new HashMap<>()
                    : objectMapper.readValue(flag.getRules(), new TypeReference<Map<String, Object>>() {
            });

            if (patch.containsKey("rolloutPct")) {
                rulesMap.put("rolloutPct", patch.get("rolloutPct"));
            }
            if (patch.containsKey("status")) {
                rulesMap.put("status", patch.get("status"));
                String status = (String) patch.get("status");
                flag.setEnabled("active".equals(status));
            }
            if (patch.containsKey("tenantOverrides")) {
                rulesMap.put("tenantOverrides", patch.get("tenantOverrides"));
            }

            flag.setRules(objectMapper.writeValueAsString(rulesMap));
            flag.setUpdatedAt(OffsetDateTime.now());
            featureFlagRepository.save(flag);

            return toDto(flag);
        } catch (Exception e) {
            log.error("[FeatureFlagService] Failed to parse/update flag rules map", e);
            throw new RuntimeException("Failed to update feature flag rules", e);
        }
    }

    @SuppressWarnings("unchecked")
    private FeatureFlagDto toDto(FeatureFlag flag) {
        int rolloutPct = 0;
        String status = flag.isEnabled() ? "active" : "inactive";
        List<String> overrides = new ArrayList<>();

        if (flag.getRules() != null) {
            try {
                Map<String, Object> rulesMap = objectMapper.readValue(flag.getRules(),
                        new TypeReference<Map<String, Object>>() {
                        });
                if (rulesMap.containsKey("rolloutPct")) {
                    rolloutPct = ((Number) rulesMap.get("rolloutPct")).intValue();
                }
                if (rulesMap.containsKey("status")) {
                    status = (String) rulesMap.get("status");
                }
                if (rulesMap.containsKey("tenantOverrides")) {
                    overrides = (List<String>) rulesMap.get("tenantOverrides");
                }
            } catch (Exception ignored) {
            }
        }
        return new FeatureFlagDto(flag.getKey(), flag.getDescription(), rolloutPct, status, overrides);
    }

    private void seedDefaultFlags(UUID tenantId) {
        String[] keys = { "new-checkout-flow", "es-recommendations", "bulk-order-import", "legacy-pricing-engine",
                "otel-traces" };
        String[] desc = {
                "Redesigned multi-step checkout with address autocomplete",
                "ML-powered Elasticsearch product recommendations",
                "CSV bulk order import with validation pipeline",
                "Old tiered pricing engine (being phased out)",
                "OpenTelemetry trace export to Grafana Tempo"
        };
        int[] rollouts = { 35, 10, 100, 0, 50 };
        String[] statuses = { "active", "active", "active", "kill_switched", "inactive" };

        for (int i = 0; i < keys.length; i++) {
            Map<String, Object> rules = Map.of(
                    "rolloutPct", rollouts[i],
                    "status", statuses[i],
                    "tenantOverrides", new ArrayList<>());
            try {
                FeatureFlag flag = FeatureFlag.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .key(keys[i])
                        .description(desc[i])
                        .enabled("active".equals(statuses[i]))
                        .rules(objectMapper.writeValueAsString(rules))
                        .createdAt(OffsetDateTime.now())
                        .build();
                featureFlagRepository.save(flag);
            } catch (Exception e) {
                log.warn("[FeatureFlagService] Failed to seed default flag key: {}", keys[i], e);
            }
        }
    }
}
