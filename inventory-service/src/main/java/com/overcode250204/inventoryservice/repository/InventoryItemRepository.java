package com.overcode250204.inventoryservice.repository;

import com.overcode250204.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    Optional<InventoryItem> findByTenantIdAndSku(UUID tenantId, String sku);

    List<InventoryItem> findByTenantId(UUID tenantId);
}
