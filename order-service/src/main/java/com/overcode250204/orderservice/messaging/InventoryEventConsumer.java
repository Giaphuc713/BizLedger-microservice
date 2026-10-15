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
 * InventoryEventConsumer — listens to changes in inventory-service-java.
 * ──────────────────────────────────────────────────────────────────────────
 * Processes stock reservation status callbacks to advance the Saga flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {

    private final IOrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory.events", groupId = "order-group")
    @IdempotentInbox(eventIdSpel = "#payload.eventId()", eventType = "inventory.events")
    public void consumeInventoryEvents(String message) {
        log.info("[Kafka Consumer] Received inventory event: {}", message);

        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

            MDC.put("traceId", envelope.traceId());
            MDC.put("tenantId", envelope.tenantId());

            UUID tenantId = UUID.fromString(envelope.tenantId());
            Map<String, Object> data = envelope.data();
            UUID orderId = UUID.fromString((String) data.get("orderId"));

            switch (envelope.eventType()) {
                case "inventory.reserved" -> orderService.handleInventoryReserved(tenantId, orderId);
                case "inventory.reserve_failed" -> {
                    String reason = (String) data.get("reason");
                    orderService.handleInventoryFailed(tenantId, orderId, reason);
                }
                default -> log.debug("Ignored inventory event: {}", envelope.eventType());
            }

        } catch (Exception e) {
            log.error("Failed to process inventory event callback", e);
        } finally {
            MDC.clear();
        }
    }
}
