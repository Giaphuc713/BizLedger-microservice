package com.overcode250204.orderservice.repository;

import com.overcode250204.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);
}
