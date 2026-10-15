package com.overcode250204.paymentservice.exception;

import com.overcode250204.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * PaymentErrorCode — all error codes for the payment service.
 *
 * Format: PAYMENT-[3-digit-number]
 * 1xx = Not Found
 * 2xx = Conflict / Duplicate
 * 3xx = Invalid State
 * 4xx = Forbidden
 */
public enum PaymentErrorCode implements ErrorCode {

    // 1xx — Not Found
    INVOICE_NOT_FOUND("PAYMENT-101", "Invoice not found", HttpStatus.NOT_FOUND),
    PAYMENT_TRANSACTION_NOT_FOUND("PAYMENT-102", "Payment transaction not found", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_NOT_FOUND("PAYMENT-103", "Subscription not found", HttpStatus.NOT_FOUND),

    // 2xx — Conflict / Duplicate
    INVOICE_ALREADY_PAID("PAYMENT-201", "Invoice has already been paid", HttpStatus.CONFLICT),
    PAYMENT_ALREADY_PROCESSED("PAYMENT-202", "Payment has already been processed", HttpStatus.CONFLICT),

    // 3xx — Invalid State
    REFUND_NOT_ELIGIBLE("PAYMENT-301", "Payment is not eligible for refund in its current status",
            HttpStatus.UNPROCESSABLE_ENTITY),

    // 4xx — Forbidden
    CROSS_TENANT_ACCESS("PAYMENT-401", "Access to another tenant's payment resource is forbidden",
            HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    PaymentErrorCode(String code, String message, HttpStatus httpStatus) {
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
