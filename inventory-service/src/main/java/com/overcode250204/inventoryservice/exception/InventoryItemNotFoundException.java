package com.overcode250204.inventoryservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;
import java.util.UUID;

/** Thrown when no inventory item exists for the given product. */
public class InventoryItemNotFoundException extends ResourceNotFoundException {
    public InventoryItemNotFoundException(UUID productId) {
        super(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND,
                "No inventory found for product: " + productId);
    }
}
