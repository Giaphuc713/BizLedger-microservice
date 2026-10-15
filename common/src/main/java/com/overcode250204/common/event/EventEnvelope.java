package com.overcode250204.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * EventEnvelope — Standard Kafka event contract for ALL BizLedger events.
 * ─────────────────────────────────────────────────────────────────────────
 * Every event published to Kafka MUST be wrapped in this envelope.
 * This ensures consistent shape across Java and C# consumers.
 *
 * Required fields per spec:
 * eventId, eventType, eventVersion, occurredAt, source, tenantId, data
 *
 * Topic naming: {domain}.events (e.g. order.events, inventory.events)
 * Event type: {domain}.{verb} (e.g. order.approval_requested)
 *
 * Interview note:
 * "We use an event envelope pattern so every consumer knows exactly
 * where to find traceId, tenantId, and eventId without knowing the
 * specific domain payload schema. This is the basis of our idempotency
 * and audit trail across the fleet."
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope(
        String eventId, // Unique event ID (ULIDv4 recommended, UUID acceptable)
        String eventType, // e.g. "order.approval_requested"
        int eventVersion, // Schema version, start at 1
        OffsetDateTime occurredAt,
        String source, // Service name that produced the event
        String traceId, // Propagated from X-Trace-Id header
        String tenantId, // Always tenant-scoped
        Map<String, Object> data // Domain-specific payload
) {
    /** Convenience factory — generates eventId and sets occurredAt = now */
    public static EventEnvelope of(
            String eventType,
            String source,
            String traceId,
            String tenantId,
            Map<String, Object> data) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                1,
                OffsetDateTime.now(),
                source,
                traceId,
                tenantId,
                data);
    }

    /**
     * Extracts value from the data map as a String, or null if key does not exist
     */
    public String getString(String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extracts value from the data map as a UUID, or null if key is missing or not
     * a valid UUID string
     */
    public UUID getUuid(String key) {
        String val = getString(key);
        if (val == null) {
            return null;
        }
        try {
            return UUID.fromString(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Checks if the data map contains the specified key */
    public boolean has(String key) {
        return data != null && data.containsKey(key);
    }
}
