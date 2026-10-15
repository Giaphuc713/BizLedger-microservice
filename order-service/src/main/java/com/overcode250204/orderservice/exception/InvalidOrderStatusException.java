package com.overcode250204.orderservice.exception;

import com.overcode250204.common.exception.InvalidStateException;
import java.util.UUID;

/** Thrown when an operation is invalid for the current order status. */
public class InvalidOrderStatusException extends InvalidStateException {
    public InvalidOrderStatusException(UUID orderId, String currentStatus, String requiredStatus) {
        super(OrderErrorCode.INVALID_ORDER_STATUS,
                String.format("Order %s is in status [%s] but requires [%s]",
                        orderId, currentStatus, requiredStatus));
    }
}
