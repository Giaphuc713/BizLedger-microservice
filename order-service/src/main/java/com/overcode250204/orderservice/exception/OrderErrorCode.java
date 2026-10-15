package com.overcode250204.orderservice.exception;

import com.overcode250204.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * OrderErrorCode — all error codes for the order service.
 *
 * Format: ORDER-[3-digit-number]
 * 1xx = Not Found
 * 2xx = Conflict / Duplicate
 * 3xx = Invalid State / Transition
 * 4xx = Forbidden
 */
public enum OrderErrorCode implements ErrorCode {

    // 1xx — Not Found
    ORDER_NOT_FOUND("ORDER-101", "Order not found", HttpStatus.NOT_FOUND),

    // 3xx — Invalid State
    INVALID_ORDER_STATUS("ORDER-301", "Operation not allowed in current order status", HttpStatus.UNPROCESSABLE_ENTITY),
    ORDER_NOT_CANCELLABLE("ORDER-302", "Order cannot be cancelled in its current status",
            HttpStatus.UNPROCESSABLE_ENTITY),
    ORDER_ALREADY_PAID("ORDER-303", "Order has already been paid", HttpStatus.UNPROCESSABLE_ENTITY),

    // 4xx — Forbidden
    CROSS_TENANT_ACCESS("ORDER-401", "Access to another tenant's order is forbidden", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    OrderErrorCode(String code, String message, HttpStatus httpStatus) {
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
