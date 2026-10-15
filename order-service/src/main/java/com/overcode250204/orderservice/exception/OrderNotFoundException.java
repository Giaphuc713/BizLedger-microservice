package com.overcode250204.orderservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;
import java.util.UUID;

/** Thrown when an order cannot be found for the given id and tenant. */
public class OrderNotFoundException extends ResourceNotFoundException {
    public OrderNotFoundException(UUID orderId) {
        super(OrderErrorCode.ORDER_NOT_FOUND,
                "Order not found: " + orderId);
    }
}
