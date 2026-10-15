package com.overcode250204.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        @NotNull(message = "Product ID is required") UUID productId,
        @NotNull(message = "SKU is required") String sku,
        @NotNull(message = "Product name is required") String name,
        @Min(value = 1, message = "Quantity must be at least 1") int quantity,
        @NotNull(message = "Unit price is required") BigDecimal unitPrice) {
}
