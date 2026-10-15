package com.overcode250204.inventoryservice.exception;

import com.overcode250204.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * InventoryErrorCode — all error codes for the inventory service.
 *
 * Format: INVENTORY-[3-digit-number]
 * 1xx = Not Found
 * 3xx = Invalid State
 */
public enum InventoryErrorCode implements ErrorCode {

    // 1xx — Not Found
    INVENTORY_ITEM_NOT_FOUND("INVENTORY-101", "Inventory item not found for product", HttpStatus.NOT_FOUND),
    RESERVATION_NOT_FOUND("INVENTORY-102", "Stock reservation not found", HttpStatus.NOT_FOUND),

    // 3xx — Invalid State
    INSUFFICIENT_STOCK("INVENTORY-301", "Insufficient stock available", HttpStatus.UNPROCESSABLE_ENTITY),
    RESERVATION_ALREADY_RELEASED("INVENTORY-302", "Stock reservation has already been released",
            HttpStatus.UNPROCESSABLE_ENTITY),

    // 4xx — Forbidden
    CROSS_TENANT_ACCESS("INVENTORY-401", "Access to another tenant's inventory is forbidden", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    InventoryErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
