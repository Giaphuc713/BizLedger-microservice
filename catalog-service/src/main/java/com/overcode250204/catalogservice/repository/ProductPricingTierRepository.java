package com.overcode250204.catalogservice.repository;

import com.overcode250204.catalogservice.entity.ProductPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductPricingTierRepository extends JpaRepository<ProductPricingTier, UUID> {
    List<ProductPricingTier> findByTenantIdAndProductId(UUID tenantId, UUID productId);
}
