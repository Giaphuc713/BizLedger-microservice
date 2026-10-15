package com.overcode250204.identityservice.repository;

import com.overcode250204.identityservice.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {
    List<FeatureFlag> findByTenantId(UUID tenantId);

    Optional<FeatureFlag> findByTenantIdAndKey(UUID tenantId, String key);
}
