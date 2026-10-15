package com.overcode250204.inventoryservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;
import java.util.UUID;

/** Thrown when a stock reservation cannot be found for the given ID. */
public class StockReservationNotFoundException extends ResourceNotFoundException {
    public StockReservationNotFoundException(UUID reservationId) {
        super(InventoryErrorCode.RESERVATION_NOT_FOUND,
                "Stock reservation not found: " + reservationId);
    }
}
