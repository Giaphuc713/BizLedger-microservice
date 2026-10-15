package com.overcode250204.orderservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID tenantId,
        UUID customerId,
        String currency,
        BigDecimal totalAmount,
        String status,
        List<OrderItemDto> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
