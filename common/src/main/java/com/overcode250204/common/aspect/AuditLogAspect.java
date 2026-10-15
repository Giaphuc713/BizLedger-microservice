package com.overcode250204.common.aspect;

import com.overcode250204.common.annotation.AuditLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * AuditLogAspect — AOP interceptor for @AuditLog methods.
 * ─────────────────────────────────────────────────────────
 * Shared across ALL Java services via bizledger-common dependency.
 *
 * Emits structured AUDIT log with:
 * action, resource, outcome (SUCCESS/FAILURE), durationMs,
 * userId, tenantId, traceId (from MDC), occurredAt
 *
 * MDC keys are populated by:
 * - Java services: JwtAuthenticationFilter reads X-User-Id / X-Tenant-Id
 * headers
 * - API Gateway: LoggingFilter sets X-Trace-Id, propagates downstream
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Around("@annotation(com.overcode250204.common.annotation.AuditLog)")
    public Object auditAroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        AuditLog annotation = sig.getMethod().getAnnotation(AuditLog.class);

        String action = annotation.action();
        String resource = annotation.resource();
        String description = annotation.description();
        String methodRef = sig.getDeclaringType().getSimpleName() + "." + sig.getName();

        String userId = MDC.get("userId");
        String tenantId = MDC.get("tenantId");
        String traceId = MDC.get("traceId");

        Instant start = Instant.now();

        try {
            Object result = joinPoint.proceed();
            long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();

            auditLog.info(
                    "AUDIT | action={} resource={} outcome=SUCCESS method={} " +
                            "userId={} tenantId={} traceId={} durationMs={} occurredAt={} description={}",
                    action, resource, methodRef,
                    orAnon(userId), orNone(tenantId), orNone(traceId),
                    durationMs, Instant.now(), description);
            return result;

        } catch (Throwable ex) {
            long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();

            auditLog.error(
                    "AUDIT | action={} resource={} outcome=FAILURE method={} " +
                            "userId={} tenantId={} traceId={} durationMs={} error={} description={}",
                    action, resource, methodRef,
                    orAnon(userId), orNone(tenantId), orNone(traceId),
                    durationMs, ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    description);
            throw ex;
        }
    }

    private String orAnon(String v) {
        return v != null ? v : "anonymous";
    }

    private String orNone(String v) {
        return v != null ? v : "none";
    }
}
