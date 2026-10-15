package com.overcode250204.catalogservice.inbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * InboxEvent — JPA entity for the Inbox Pattern (Idempotent Consumer).
 * ──────────────────────────────────────────────────────────────────────
 * Why Inbox Pattern?
 * Kafka guarantees at-least-once delivery. This means a consumer may
 * receive the same event multiple times (rebalance, retry, replay).
 *
 * The Inbox table records every processed eventId within the same
 * transaction as the business operation. On duplicate delivery,
 * the @IdempotentInbox aspect checks the table and skips re-processing.
 *
 * All Java services that consume Kafka events use this entity.
 * Each service has its own inbox_events table (database ownership).
 */
@Entity
@Table(name = "inbox_event", indexes = @Index(name = "idx_inbox_event_id", columnList = "event_id", unique = true))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;
}
