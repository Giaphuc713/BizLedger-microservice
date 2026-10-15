package com.overcode250204.paymentservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overcode250204.common.annotation.IdempotentInbox;
import com.overcode250204.common.event.EventEnvelope;
import com.overcode250204.paymentservice.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * OrderApprovedConsumer — listens to order-service approved events.
 * ─────────────────────────────────────────────────────────────────
 * Installs invoices downstream. Secured with @IdempotentInbox.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderApprovedConsumer {

    private final IPaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.events", groupId = "payment-group")
    @IdempotentInbox(eventIdSpel = "#payload.eventId()", eventType = "order.events")
    public void consumeOrderApproved(String message) {
        log.info("[Kafka Consumer] Received order event: {}", message);

        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

            MDC.put("traceId", envelope.traceId());
            MDC.put("tenantId", envelope.tenantId());

            UUID tenantId = UUID.fromString(envelope.tenantId());
            Map<String, Object> data = envelope.data();

            if ("order.approved".equals(envelope.eventType())) {
                UUID orderId = UUID.fromString((String) data.get("orderId"));
                BigDecimal amount = BigDecimal.valueOf(((Number) data.get("totalAmount")).doubleValue());
                String currency = (String) data.get("currency");

                paymentService.createInvoice(tenantId, orderId, amount, currency, envelope.traceId());
            }

        } catch (Exception e) {
            log.error("Failed to parse and resolve order approvals", e);
        } finally {
            MDC.clear();
        }
    }
}
