package com.overcode250204.inventoryservice.exception;

import com.overcode250204.common.exception.InvalidStateException;
import java.util.UUID;

/**
 * Thrown when there is not enough available stock to fulfill a reservation
 * request.
 */
public class InsufficientStockException extends InvalidStateException {
    public InsufficientStockException(UUID productId, String sku, int available, int requested) {
        super(InventoryErrorCode.INSUFFICIENT_STOCK,
                String.format("Insufficient stock for product %s (SKU: %s). Available: %d, Requested: %d",
                        productId, sku, available, requested));
    }
}
