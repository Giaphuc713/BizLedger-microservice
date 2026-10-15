package com.overcode250204.inventoryservice.repository;

import com.overcode250204.inventoryservice.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
    List<StockReservation> findByOrderId(UUID orderId);

    Optional<StockReservation> findByOrderIdAndProductId(UUID orderId, UUID productId);

    List<StockReservation> findByTenantId(UUID tenantId);
}
