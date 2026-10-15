package com.overcode250204.paymentservice.exception;

import com.overcode250204.common.exception.ResourceNotFoundException;
import java.util.UUID;

/** Thrown when an invoice cannot be found for the given id and tenant. */
public class InvoiceNotFoundException extends ResourceNotFoundException {
    public InvoiceNotFoundException(UUID invoiceId) {
        super(PaymentErrorCode.INVOICE_NOT_FOUND, "Invoice not found: " + invoiceId);
    }
}
