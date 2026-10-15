package com.overcode250204.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        UUID customerId,
        @NotEmpty(message = "Order must contain at least one item") @Valid List<OrderItemDto> items) {
}
