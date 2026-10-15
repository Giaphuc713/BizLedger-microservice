package com.overcode250204.common.aspect;

import com.overcode250204.common.annotation.IdempotentInbox;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * IdempotentInboxAspect — Inbox Pattern enforcement via AOP.
 * ───────────────────────────────────────────────────────────
 * Shared by ALL Java Kafka consumer services.
 *
 * Algorithm:
 * 1. Extract eventId from method parameter using SpEL
 * 2. SELECT COUNT(*) FROM inbox_events WHERE event_id = ?
 * 3. If count > 0 → skip (already processed) → return null
 * 4. Proceed with business method
 * 5. INSERT INTO inbox_events (event_id, event_type, processed_at)
 * — all within the same transaction as the business operation
 *
 * If the business method throws → inbox INSERT is rolled back →
 * the event will be retried from Kafka → correct at-least-once behavior.
 *
 * Interview note:
 * "The aspect runs the CHECK and the WRITE in the same transaction as
 * the business logic. This is the key insight: if we wrote to inbox
 * in a separate transaction, we could still get duplicate processing
 * under certain failure scenarios."
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentInboxAspect {

    private final EntityManager entityManager;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(idempotentInbox)")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object enforceIdempotency(
            ProceedingJoinPoint joinPoint,
            IdempotentInbox idempotentInbox) throws Throwable {

        // ── 1. Resolve eventId via SpEL ──────────────────────────────────────
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            ctx.setVariable(paramNames[i], args[i]);
        }

        String eventId = parser.parseExpression(idempotentInbox.eventIdSpel()).getValue(ctx, String.class);
        String eventType = idempotentInbox.eventType();

        // ── 2. Check inbox table ──────────────────────────────────────────────
        Number count = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM inbox_events WHERE event_id = ?")
                .setParameter(1, eventId)
                .getSingleResult();

        if (count.longValue() > 0) {
            log.warn("[Inbox] Duplicate event skipped | eventId={} eventType={}", eventId, eventType);
            return null; // No-op — Kafka ack still happens
        }

        // ── 3. Execute business logic ─────────────────────────────────────────
        Object result = joinPoint.proceed();

        // ── 4. Mark as processed (atomic with business operation) ─────────────
        entityManager
                .createNativeQuery(
                        "INSERT INTO inbox_events (event_id, event_type, processed_at) VALUES (?, ?, ?)")
                .setParameter(1, eventId)
                .setParameter(2, eventType)
                .setParameter(3, OffsetDateTime.now())
                .executeUpdate();

        log.debug("[Inbox] Event processed | eventId={} eventType={}", eventId, eventType);
        return result;
    }
}
