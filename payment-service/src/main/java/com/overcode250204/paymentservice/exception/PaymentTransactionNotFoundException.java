package com.overcode250204.paymentservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;
import java.util.UUID;

/** Thrown when a payment transaction cannot be found. */
public class PaymentTransactionNotFoundException extends ResourceNotFoundException {
    public PaymentTransactionNotFoundException(UUID paymentId) {
        super(PaymentErrorCode.PAYMENT_TRANSACTION_NOT_FOUND,
                "Payment transaction not found: " + paymentId);
    }
}
