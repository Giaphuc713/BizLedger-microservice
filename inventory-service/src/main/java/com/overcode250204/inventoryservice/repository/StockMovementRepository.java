package com.overcode250204.inventoryservice.repository;

import com.overcode250204.inventoryservice.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByTenantId(UUID tenantId);
}
