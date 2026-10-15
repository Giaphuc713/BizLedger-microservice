package com.overcode250204.catalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(UUID categoryId,
                                   @NotBlank(message = "SKU is required") String sku,
                                   @NotBlank(message = "Product name is required") String name,
                                   String description,
                                   String unit,
                                   @NotNull(message = "Price is required") @PositiveOrZero(message = "Price must be positive or zero") BigDecimal price,
                                   String currency) {
}
