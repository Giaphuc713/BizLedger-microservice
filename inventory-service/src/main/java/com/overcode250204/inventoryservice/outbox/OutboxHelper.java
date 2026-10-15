package com.overcode250204.inventoryservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.overcode250204.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * OutboxHelper — Core utility to publish and serialize domain events to the
 * Outbox table.
 * Shared across catalog, order, payment, inventory, and identity microservices.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxHelper {

    private final OutboxEventRepository outboxRepository;

    private final ObjectMapper objectMapper;

    /**
     * Persist an event to the outbox database table.
     */
    @Transactional
    public void saveEvent(
            String topic,
            String eventType,
            String sourceService,
            UUID tenantId,
            Map<String, Object> data) {
        String traceId = MDC.get("traceId");
        saveEvent(topic, eventType, sourceService, tenantId, traceId, data);
    }

    /**
     * Persist an event to the outbox database table with a custom trace ID.
     */
    @Transactional
    public void saveEvent(
            String topic,
            String eventType,
            String sourceService,
            UUID tenantId,
            String traceId,
            Map<String, Object> data) {
        try {
            EventEnvelope envelope = EventEnvelope.of(
                    eventType,
                    sourceService,
                    traceId,
                    tenantId != null ? tenantId.toString() : null,
                    data);

            String payload = objectMapper.writeValueAsString(envelope);
            OutboxEvent outbox = OutboxEvent.pending(
                    envelope.eventId(),
                    envelope.eventType(),
                    topic,
                    payload);

            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Outbox serialization failure for topic={}", topic, e);
            throw new RuntimeException("Outbox transaction failed during serialization", e);
        }
    }
}
