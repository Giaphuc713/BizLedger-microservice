package com.overcode250204.orderservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overcode250204.common.annotation.IdempotentInbox;
import com.overcode250204.common.event.EventEnvelope;
import com.overcode250204.orderservice.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * PaymentEventConsumer — listens to payment events from payment-service-java.
 * ──────────────────────────────────────────────────────────────────────────
 * Processes payment outcomes to mark Orders as PAID (Saga success).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final IOrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.events", groupId = "order-group")
    @IdempotentInbox(eventIdSpel = "#payload.eventId()", eventType = "payment.events")
    public void consumePaymentEvents(String message) {
        log.info("[Kafka Consumer] Received payment event: {}", message);

        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

            MDC.put("traceId", envelope.traceId());
            MDC.put("tenantId", envelope.tenantId());

            UUID tenantId = UUID.fromString(envelope.tenantId());
            Map<String, Object> data = envelope.data();
            UUID orderId = UUID.fromString((String) data.get("orderId"));

            if ("payment.succeeded".equals(envelope.eventType())) {
                orderService.handlePaymentSucceeded(tenantId, orderId);
            } else {
                log.debug("Ignored details for payment event: {}", envelope.eventType());
            }

        } catch (Exception e) {
            log.error("Failed to process payment event callback", e);
        } finally {
            MDC.clear();
        }
    }
}
