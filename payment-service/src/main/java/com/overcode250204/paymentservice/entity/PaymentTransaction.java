package com.overcode250204.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // BANK_TRANSFER, MOMO, CREDIT_CARD

    @Column(name = "reference_no", nullable = false, unique = true, length = 100)
    private String referenceNo;

    @Column(nullable = false, length = 50)
    private String status; // SUCCESS, FAILED

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;
}
