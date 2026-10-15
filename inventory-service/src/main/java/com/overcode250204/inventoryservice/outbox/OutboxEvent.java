package com.overcode250204.inventoryservice.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * OutboxEvent — JPA entity for the Transactional Outbox Pattern.
 * ──────────────────────────────────────────────────────────────
 * Why Outbox Pattern?
 * Without it: if service saves to DB and then Kafka publish fails,
 * the event is lost permanently → data inconsistency.
 *
 * With Outbox: save event to outbox_events table IN THE SAME TRANSACTION
 * as the business data. A separate scheduler reads and publishes it to Kafka.
 * If publish fails, scheduler retries. Guaranteed at-least-once delivery.
 *
 * Shared by ALL Java services — each creates its own outbox_events table
 * with Flyway but maps to this same entity class.
 *
 * Status lifecycle: PENDING → PUBLISHED | FAILED
 *
 * Interview note:
 * "Outbox Pattern is the only way to achieve exactly-once semantics
 * between a database write and a Kafka publish without distributed
 * transactions (2PC). We write to outbox atomically, then publish
 * asynchronously with retry."
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    /** JSON-serialized EventEnvelope */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** PENDING | PUBLISHED | FAILED */
    @Column(nullable = false)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /** Factory for creating a new PENDING outbox event */
    public static OutboxEvent pending(String eventId, String eventType, String topic, String payload) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .eventType(eventType)
                .topic(topic)
                .payload(payload)
                .status("PENDING")
                .retryCount(0)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
