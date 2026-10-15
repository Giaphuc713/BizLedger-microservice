package com.overcode250204.paymentservice.service.impl;

import com.overcode250204.paymentservice.exception.InvoiceNotFoundException;
import com.overcode250204.paymentservice.exception.PaymentTransactionNotFoundException;
import com.overcode250204.paymentservice.exception.UnauthorizedPaymentException;
import com.overcode250204.common.annotation.AuditLog;

import com.overcode250204.paymentservice.entity.Invoice;
import com.overcode250204.paymentservice.entity.PaymentTransaction;
import com.overcode250204.paymentservice.outbox.OutboxHelper;
import com.overcode250204.paymentservice.repository.InvoiceRepository;
import com.overcode250204.paymentservice.repository.PaymentTransactionRepository;
import com.overcode250204.paymentservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final OutboxHelper outboxHelper;

    @Override
    @Transactional
    public void createInvoice(UUID tenantId, UUID orderId, BigDecimal amount, String currency, String traceId) {
        log.info("[Saga Participant] Creating invoice for order={} tenant={}", orderId, tenantId);

        Invoice invoice = Invoice.builder()
                .tenantId(tenantId)
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .status("PENDING_PAYMENT")
                .build();

        invoiceRepository.save(invoice);

        publishOutbox(tenantId, "invoice.created", invoice.getId(), traceId, Map.of(
                "invoiceId", invoice.getId().toString(),
                "orderId", orderId.toString(),
                "amount", amount.doubleValue(),
                "tenantId", tenantId.toString()));
    }

    @Override
    @Transactional
    @AuditLog(action = "PAYMENT_PROCESSED", resource = "payment")
    public void processPayment(UUID tenantId, UUID invoiceId, BigDecimal amount, String method, String referenceNo,
                               String traceId) {
        log.info("[Payment] Processing payment transaction for invoice={} referenceNo={}", invoiceId,
                referenceNo);

        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if ("PAID".equals(invoice.getStatus())) {
            log.warn("Invoice is already paid: {}", invoiceId);
            return;
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .invoiceId(invoiceId)
                .amount(amount)
                .paymentMethod(method)
                .referenceNo(referenceNo)
                .status("SUCCESS")
                .processedAt(OffsetDateTime.now())
                .build();

        transactionRepository.save(transaction);

        invoice.setStatus("PAID");
        invoiceRepository.save(invoice);

        publishOutbox(tenantId, "payment.succeeded", invoice.getOrderId(), traceId, Map.of(
                "invoiceId", invoiceId.toString(),
                "orderId", invoice.getOrderId().toString(),
                "tenantId", tenantId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransaction> getPayments(UUID tenantId) {
        return transactionRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoices(UUID tenantId) {
        return invoiceRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional
    public Map<String, Object> refundPayment(UUID tenantId, UUID paymentId, BigDecimal amount) {
        PaymentTransaction tx = transactionRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentTransactionNotFoundException(paymentId));

        if (!tx.getTenantId().equals(tenantId)) {
            throw new UnauthorizedPaymentException(tenantId, paymentId);
        }

        tx.setStatus("REFUNDED");
        transactionRepository.save(tx);

        return Map.of(
                "refundId", UUID.randomUUID().toString(),
                "paymentId", paymentId.toString(),
                "status", "SUCCESS",
                "refundedAmount", amount);
    }

    @Override
    public List<Map<String, Object>> getSubscriptions(UUID tenantId) {
        return List.of(
                Map.of("id", UUID.randomUUID().toString(), "customerId", UUID.randomUUID().toString(),
                        "plan",
                        "Enterprise Pro", "amount", 599.00, "status", "active", "nextBillingAt",
                        OffsetDateTime.now().plusMonths(1).toString()),
                Map.of("id", UUID.randomUUID().toString(), "customerId", UUID.randomUUID().toString(),
                        "plan",
                        "Starter Suite", "amount", 49.00, "status", "active", "nextBillingAt",
                        OffsetDateTime.now().plusMonths(1).toString()));
    }

    @Override
    public Map<String, Object> createSubscription(UUID tenantId, Map<String, Object> body) {
        return Map.of(
                "id", UUID.randomUUID().toString(),
                "customerId", body.getOrDefault("customerId", UUID.randomUUID().toString()),
                "plan", body.getOrDefault("plan", "Standard Tier"),
                "amount", body.getOrDefault("amount", 99.00),
                "status", "active",
                "createdAt", OffsetDateTime.now().toString(),
                "nextBillingAt", OffsetDateTime.now().plusMonths(1).toString());
    }

    @Override
    public Map<String, Object> getFxRates(UUID tenantId) {
        return Map.of(
                "base", "USD",
                "rates", Map.of(
                        "EUR", 0.92,
                        "VND", 25415.00,
                        "SGD", 1.34,
                        "JPY", 156.42),
                "updatedAt", OffsetDateTime.now().toString());
    }

    private void publishOutbox(UUID tenantId, String eventType, UUID entityId, String traceId,
                               Map<String, Object> data) {
        outboxHelper.saveEvent("payment.events", eventType, "payment-service", tenantId, traceId, data);
    }
}
