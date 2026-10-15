package com.overcode250204.catalogservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID tenantId,
        UUID categoryId,
        String sku,
        String name,
        String description,
        String unit,
        BigDecimal price,
        String currency,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
