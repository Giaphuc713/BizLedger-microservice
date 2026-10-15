package com.overcode250204.paymentservice.repository;

import com.overcode250204.paymentservice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByOrderId(UUID orderId);

    Optional<Invoice> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Invoice> findByTenantId(UUID tenantId);
}
