package com.overcode250204.identityservice.service;

import java.util.Map;
import java.util.UUID;

/**
 * IOutboxService — Service interface defining contracts for transactional
 * Outbox Pattern event publishing to ensure eventual consistency.
 */
public interface IOutboxService {

    /**
     * Persists a payload envelope event to the outbox database table.
     *
     * @param topic     the target message broker destination queue topic
     * @param eventType the classification name of the event
     * @param tenantId  the tenant database identifier
     * @param traceId   tracing correlation context identifier
     * @param data      mapping of key-value parameters representing event payload
     *                  details
     */
    void saveEvent(
            String topic,
            String eventType,
            UUID tenantId,
            String traceId,
            Map<String, Object> data);
}
