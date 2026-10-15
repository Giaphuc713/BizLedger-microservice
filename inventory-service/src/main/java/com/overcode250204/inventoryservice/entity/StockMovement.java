package com.overcode250204.inventoryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType; // IN, OUT, ADJUST, RESERVE, RELEASE

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reference_id", length = 100)
    private String referenceId; // orderId, etc

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
