package com.overcode250204.paymentservice.exception;

import com.overcode250204.common.exception.ForbiddenOperationException;
import java.util.UUID;

/**
 * Thrown when a tenant attempts to access a payment transaction belonging to
 * another tenant.
 */
public class UnauthorizedPaymentException extends ForbiddenOperationException {
    public UnauthorizedPaymentException(UUID tenantId, UUID paymentId) {
        super(PaymentErrorCode.CROSS_TENANT_ACCESS,
                String.format("Tenant %s is not permitted to access payment transaction %s", tenantId, paymentId));
    }
}
