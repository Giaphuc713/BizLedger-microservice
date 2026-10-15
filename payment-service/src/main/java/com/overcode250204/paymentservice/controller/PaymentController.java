package com.overcode250204.paymentservice.controller;

import com.overcode250204.common.web.ApiResponse;
import com.overcode250204.paymentservice.entity.Invoice;
import com.overcode250204.paymentservice.entity.PaymentTransaction;
import com.overcode250204.paymentservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/pay")
    @PreAuthorize("hasAuthority('payment:write')")
    public ResponseEntity<ApiResponse<Void>> payInvoice(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam UUID invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam String method,
            @RequestParam String referenceNo,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String effectiveTraceId = traceId != null ? traceId : UUID.randomUUID().toString();
        paymentService.processPayment(tenantId, invoiceId, amount, method, referenceNo, effectiveTraceId);
        return ResponseEntity.ok(ApiResponse.noContent("Payment processed successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payment:read')")
    public ResponseEntity<ApiResponse<List<PaymentTransaction>>> getPayments(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayments(tenantId)));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('payment:read')")
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoices(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getInvoices(tenantId)));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAuthority('payment:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refundPayment(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable UUID paymentId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully",
                paymentService.refundPayment(tenantId, paymentId, amount)));
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("hasAuthority('payment:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubscriptions(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getSubscriptions(tenantId)));
    }

    @PostMapping("/subscriptions")
    @PreAuthorize("hasAuthority('payment:write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSubscription(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.success("Subscription created successfully",
                paymentService.createSubscription(tenantId, body)));
    }

    @GetMapping("/fx-rates")
    @PreAuthorize("hasAuthority('payment:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFxRates(
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getFxRates(tenantId)));
    }
}
