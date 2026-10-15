package com.overcode250204.identityservice.outbox;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * OutboxPublisher — shared scheduled publisher for the Transactional Outbox
 * pattern.
 * ────────────────────────────────────────────────────────────────────────────────────
 * All Java microservices share this single implementation via bizledger-common.
 * Each service brings its own OutboxEventRepository bean (backed by its own DB
 * schema).
 *
 * Spring auto-wires the correct OutboxEventRepository instance from each
 * service's
 * application context, so the publisher always queries that service's
 * outbox_events table.
 *
 * Lifecycle: PENDING → PUBLISHED | FAILED (after 5 retries)
 *
 * Interview note:
 * "We extract the outbox publisher to the shared library to enforce a single,
 * audited implementation of the at-least-once Kafka delivery guarantee.
 * Configuration (fixedDelay) can be overridden per-service
 * via @ConditionalOnProperty."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${bizledger.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepository.findPending();
        if (events.isEmpty()) {
            return;
        }

        log.debug("[Outbox] Polled {} pending events to publish", events.size());

        for (OutboxEvent event : events) {
            try {
                // Block until broker ack — At-least-once delivery guarantee
                kafkaTemplate.send(event.getTopic(), event.getEventId(), event.getPayload()).get();

                event.setStatus("PUBLISHED");
                event.setPublishedAt(OffsetDateTime.now());
                outboxRepository.save(event);

                log.info("[Outbox] Published event | eventId={} type={} topic={}",
                        event.getEventId(), event.getEventType(), event.getTopic());

            } catch (Exception e) {
                log.error("[Outbox] Failed to publish event | eventId={}", event.getEventId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                    log.warn("[Outbox] Event moved to FAILED after 5 retries | eventId={}", event.getEventId());
                }
                outboxRepository.save(event);
            }
        }
    }
}
