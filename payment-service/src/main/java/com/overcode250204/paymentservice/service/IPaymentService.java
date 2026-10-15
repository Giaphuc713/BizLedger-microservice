package com.overcode250204.paymentservice.service;

import com.overcode250204.paymentservice.entity.Invoice;
import com.overcode250204.paymentservice.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IPaymentService — Service interface defining business contracts for
 * invoicing, payment processing, and subscriptions.
 */
public interface IPaymentService {

    /**
     * Creates a new billing invoice linked to an order.
     *
     * @param tenantId the unique identifier of the tenant
     * @param orderId  the unique identifier of the order
     * @param amount   the billing amount
     * @param currency the currency code
     * @param traceId  tracing context for correlation
     */
    void createInvoice(UUID tenantId, UUID orderId, BigDecimal amount, String currency, String traceId);

    /**
     * Processes a payment transaction against a pending invoice.
     *
     * @param tenantId    the unique identifier of the tenant
     * @param invoiceId   the billing invoice identifier
     * @param amount      the transaction payment receipt amount
     * @param method      payment strategy method
     * @param referenceNo bank or gateway reference number
     * @param traceId     tracing context
     * @throws com.overcode250204.paymentservice.exception.InvoiceNotFoundException if
     *                                                                              the
     *                                                                              invoice
     *                                                                              does
     *                                                                              not
     *                                                                              exist
     * @throws com.overcode250204.common.exception.DuplicateResourceException       if
     *                                                                              the
     *                                                                              payment
     *                                                                              reference
     *                                                                              number
     *                                                                              is
     *                                                                              a
     *                                                                              duplicate
     */
    void processPayment(UUID tenantId, UUID invoiceId, BigDecimal amount, String method, String referenceNo,
                        String traceId);

    /**
     * Retrieves historical payment transactions for a given tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of transaction records
     */
    List<PaymentTransaction> getPayments(UUID tenantId);

    /**
     * Retrieves invoicing records for a given tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of invoices
     */
    List<Invoice> getInvoices(UUID tenantId);

    /**
     * Refunds mapping details of a completed payment transaction.
     *
     * @param tenantId  the unique identifier of the tenant
     * @param paymentId payment transaction unique identifier
     * @param amount    target refund money amount
     * @return outcome details mapping context
     * @throws com.overcode250204.paymentservice.exception.InvoiceNotFoundException if
     *                                                                              parent
     *                                                                              invoice
     *                                                                              is
     *                                                                              missing
     * @throws com.overcode250204.common.exception.InvalidStateException            if
     *                                                                              transaction
     *                                                                              cannot
     *                                                                              be
     *                                                                              refunded
     */
    Map<String, Object> refundPayment(UUID tenantId, UUID paymentId, BigDecimal amount);

    /**
     * Pulls active tenant consumer subscription packages.
     *
     * @param tenantId the unique identifier of the tenant
     * @return list of matching subscription packages
     */
    List<Map<String, Object>> getSubscriptions(UUID tenantId);

    /**
     * Sets up recurring billing cycles for a tenant context.
     *
     * @param tenantId the unique identifier of the tenant
     * @param body     subscription parameters mapping details
     * @return created subscription properties map
     */
    Map<String, Object> createSubscription(UUID tenantId, Map<String, Object> body);

    /**
     * Retrieves live currency exchange forex parameters.
     *
     * @param tenantId the unique identifier of the tenant
     * @return map of exchange rates
     */
    Map<String, Object> getFxRates(UUID tenantId);
}
