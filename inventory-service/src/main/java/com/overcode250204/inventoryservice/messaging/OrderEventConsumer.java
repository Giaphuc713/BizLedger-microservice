package com.overcode250204.inventoryservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overcode250204.common.annotation.IdempotentInbox;
import com.overcode250204.common.event.EventEnvelope;
import com.overcode250204.inventoryservice.service.IInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OrderEventConsumer — listens to order development events to trigger Saga
 * steps.
 * ─────────────────────────────────────────────────────────────────────────────
 * Direct integration with AOP inbox to prevent duplicate message execution.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final IInventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.events", groupId = "inventory-group")
    @IdempotentInbox(eventIdSpel = "#payload.eventId()", eventType = "order.events")
    public void consumeOrderEvents(String message) {
        log.info("[Kafka Consumer] Received order event: {}", message);

        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);

            // Establish logging context from message metadata
            MDC.put("traceId", envelope.traceId());
            MDC.put("tenantId", envelope.tenantId());

            UUID tenantId = UUID.fromString(envelope.tenantId());
            Map<String, Object> data = envelope.data();

            switch (envelope.eventType()) {
                case "order.approval_requested" -> {
                    UUID orderId = UUID.fromString((String) data.get("orderId"));
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                    inventoryService.reserveStock(tenantId, orderId, items, envelope.traceId());
                }
                case "order.cancelled" -> {
                    UUID orderId = UUID.fromString((String) data.get("orderId"));
                    inventoryService.releaseStock(tenantId, orderId, envelope.traceId());
                }
                case "order.approved" -> {
                    // Once successfully approved (and paid), complete reservation lifecycle
                    UUID orderId = UUID.fromString((String) data.get("orderId"));
                    inventoryService.commitStock(tenantId, orderId);
                }
                default -> log.debug("Ignored unhandled event: {}", envelope.eventType());
            }

        } catch (Exception e) {
            log.error("Failed to parse and handle order event message", e);
            // In production, send to Dead Letter Queue (DLQ)
        } finally {
            MDC.clear();
        }
    }
}
