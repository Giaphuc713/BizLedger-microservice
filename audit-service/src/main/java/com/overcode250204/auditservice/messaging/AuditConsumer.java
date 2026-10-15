package com.overcode250204.auditservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overcode250204.auditservice.document.AuditDocument;
import com.overcode250204.auditservice.entity.AuditEvent;
import com.overcode250204.auditservice.repository.AuditEventRepository;
import com.overcode250204.auditservice.service.impl.AuditSearchService;
import com.overcode250204.common.annotation.IdempotentInbox;
import com.overcode250204.common.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AuditConsumer — Centralized aggregation sink.
 * ───────────────────────────────────────
 * Consumes ALL domain topics, parsing structural EventEnvelope payloads and:
 * 1. Saves to PostgreSQL (AuditEvent) — durable storage, retention baseline
 * 2. Indexes into Elasticsearch (AuditDocument) — full-text search, analytics
 *
 * Dual-write strategy: ES indexing is fire-and-forget.
 * PostgreSQL write failure → rollback transaction.
 * ES index failure → logged, does NOT affect Kafka offset commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditConsumer {

    private final AuditEventRepository auditRepository;
    private final AuditSearchService auditSearchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = { "catalog.events", "inventory.events", "order.events",
            "payment.events", "identity.events" }, groupId = "audit-aggregator-group")
    @IdempotentInbox(eventIdSpel = "#payload.eventId()", eventType = "audit.events")
    public void consumeAuditEvent(String message) {
        log.info("[Audit Aggregator] Received event for aggregation: {}", message);

        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
            String payloadJson = objectMapper.writeValueAsString(envelope.data());

            // ── 1. Persist to PostgreSQL ─────────────────────────────────────
            AuditEvent auditEvent = AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .eventId(envelope.eventId())
                    .eventType(envelope.eventType())
                    .serviceName(envelope.source())
                    .traceId(envelope.traceId())
                    .tenantId(UUID.fromString(envelope.tenantId()))
                    .userId(envelope.data() != null
                            ? String.valueOf(envelope.data().getOrDefault("userId", ""))
                            : "")
                    .payload(payloadJson)
                    .timestamp(envelope.occurredAt())
                    .build();

            auditRepository.save(auditEvent);
            log.info("[Audit] Persisted to PostgreSQL | eventId={} type={}", envelope.eventId(), envelope.eventType());

            // ── 2. Index into Elasticsearch (fire-and-forget) ────────────────
            AuditDocument document = AuditDocument.builder()
                    .id(auditEvent.getId().toString())
                    .eventId(envelope.eventId())
                    .eventType(envelope.eventType())
                    .serviceName(envelope.source())
                    .traceId(envelope.traceId())
                    .tenantId(envelope.tenantId())
                    .userId(auditEvent.getUserId())
                    .payload(payloadJson)
                    .timestamp(envelope.occurredAt())
                    .build();

            auditSearchService.index(document);

        } catch (Exception e) {
            log.error("[Audit] Failed to process audit event", e);
        }
    }
}
