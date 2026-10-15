package com.overcode250204.apigatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    // Header names shared across all services
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    // MDC keys — must match what logback-spring.xml declares in <includeMdcKeyName>
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_TENANT_ID = "tenantId";

    @Override
    public int getOrder() {
        // Run before AuthenticationFilter (order 0) so traceId is set early
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // ─── 1. Resolve or generate traceId ───────────────────────────────────
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // ─── 2. Extract identity context (available after AuthenticationFilter) ─
        // At this point userId/tenantId may be absent for public routes — that's OK.
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        String tenantId = request.getHeaders().getFirst(TENANT_ID_HEADER);

        final String resolvedTraceId = traceId;

        // ─── 3. MDC: set for reactive context propagation ─────────────────────
        // NOTE: In WebFlux, MDC is set per-signal, not per-thread.
        // We use contextWrite to bind MDC for the Mono chain.
        MDC.put(MDC_TRACE_ID, resolvedTraceId);
        if (userId != null)
            MDC.put(MDC_USER_ID, userId);
        if (tenantId != null)
            MDC.put(MDC_TENANT_ID, tenantId);

        // ─── 4. Log incoming request ──────────────────────────────────────────
        String clientIp = getClientIp(request);
        log.info("→ Incoming Request | method={} path={} clientIp={} traceId={}",
                request.getMethod(),
                request.getPath().value(),
                clientIp,
                resolvedTraceId);

        // ─── 5. Mutate request: inject X-Trace-Id downstream ─────────────────
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, resolvedTraceId)
                .build();

        // ─── 6. Add X-Trace-Id to response for client correlation ────────────
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, resolvedTraceId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    // Always clear MDC to prevent pollution across requests (thread safety)
                    MDC.remove(MDC_TRACE_ID);
                    MDC.remove(MDC_SPAN_ID);
                    MDC.remove(MDC_USER_ID);
                    MDC.remove(MDC_TENANT_ID);
                });
    }

    /**
     * Resolve real client IP, respecting X-Forwarded-For (common behind load
     * balancers/AWS ALB).
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; first IP is the original
            // client
            return xForwardedFor.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
