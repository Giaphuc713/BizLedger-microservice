package com.overcode250204.common.annotation;

import java.lang.annotation.*;

/**
 * @AuditLog — Method-level annotation for automatic audit trail.
 *           ───────────────────────────────────────────────────────────────
 *           Place on any service method that represents a significant user
 *           action.
 *           AuditLogAspect intercepts and emits a structured AUDIT log line.
 *
 *           All BizLedger Java services share this annotation from common.
 *
 *           Usage:
 *           {@literal @}AuditLog(action = "ORDER_SUBMITTED", resource =
 *           "order")
 *           public OrderResponse submitOrder(UUID orderId, UUID userId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /** Business action: SCREAMING_SNAKE_CASE. E.g. USER_LOGIN, ORDER_APPROVED */
    String action();

    /** Domain resource. E.g. "auth", "order", "inventory" */
    String resource();

    /** Optional human-readable description */
    String description() default "";
}
