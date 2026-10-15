package com.overcode250204.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * AuthenticationFilter — per-route GatewayFilter
 * ───────────────────────────────────────────────
 * Applied only to protected routes (configured in YAML: "-
 * AuthenticationFilter").
 *
 * Responsibilities:
 * 1. Validate JWT Bearer token (signature, expiry).
 * 2. Extract identity claims: userId, tenantId, roles, permissions.
 * 3. Inject identity context headers downstream so each service does NOT need
 * to parse JWT.
 * 4. Forward X-Trace-Id (set by LoggingFilter) to maintain tracing continuity.
 *
 * Services downstream should trust these headers because only the gateway can
 * set them.
 * Identity headers: X-User-Id, X-Tenant-Id, X-Roles, X-Permissions, X-Trace-Id.
 *
 * Interview note:
 * "We apply a Zero Trust Gateway pattern: each downstream service reads its
 * user context
 * from trusted internal headers rather than re-parsing the JWT. This keeps
 * security logic
 * centralized in one place and reduces JWT parsing overhead across the fleet."
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    public AuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String traceId = request.getHeaders().getFirst(LoggingFilter.TRACE_ID_HEADER);


            if ("OPTIONS".equals(exchange.getRequest().getMethod().name())) {
                return chain.filter(exchange);
            }

            // ── 1. Require Authorization header ──────────────────────────────
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header | traceId={} path={}",
                        traceId, request.getPath().value());
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization scheme | traceId={}", traceId);
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // ── 2. Parse and validate JWT ─────────────────────────────────────
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.get("userId", String.class);
                String tenantId = claims.get("tenantId", String.class);
                String roles = safeJoin(claims.get("roles", Object.class));
                String perms = safeJoin(claims.get("permissions", Object.class));

                log.debug("JWT validated | traceId={} userId={} tenantId={}", traceId, userId, tenantId);

                // ── 3. Inject identity context + traceId headers downstream ──
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header(LoggingFilter.USER_ID_HEADER, userId)
                        .header(LoggingFilter.TENANT_ID_HEADER, tenantId)
                        .header("X-Roles", roles)
                        .header("X-Permissions", perms)
                        .header(LoggingFilter.TRACE_ID_HEADER, traceId != null ? traceId : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.warn("JWT validation failed | traceId={} reason={}", traceId, e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    /**
     * Safely convert claim value (might be List<String> or String) to
     * comma-separated string.
     */
    @SuppressWarnings("unchecked")
    private String safeJoin(Object value) {
        if (value == null)
            return "";
        if (value instanceof Iterable<?> it) {
            var sb = new StringBuilder();
            for (Object item : it) {
                if (!sb.isEmpty())
                    sb.append(",");
                sb.append(item);
            }
            return sb.toString();
        }
        return value.toString();
    }
}
