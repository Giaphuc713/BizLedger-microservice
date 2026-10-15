package com.overcode250204.apigatewayservice.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AccessLogFilter implements GatewayFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    public int getOrder() {
        // After LoggingFilter(-2) but before auth(-1 or 0)
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String traceId = request.getHeaders().getFirst(LoggingFilter.TRACE_ID_HEADER);
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getPath().value();
        String clientIp = getClientIp(request);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long duration = System.currentTimeMillis() - startTime;

                    // After auth filter runs, userId/tenantId are available in response headers
                    String userId = exchange.getRequest().getHeaders().getFirst(LoggingFilter.USER_ID_HEADER);
                    String tenantId = exchange.getRequest().getHeaders().getFirst(LoggingFilter.TENANT_ID_HEADER);
                    int statusCode = response.getStatusCode() != null
                            ? response.getStatusCode().value()
                            : 0;

                    log.info(
                            "ACCESS | traceId={} method={} path={} status={} durationMs={} userId={} tenantId={} clientIp={}",
                            traceId, method, path, statusCode, duration,
                            userId != null ? userId : "anonymous",
                            tenantId != null ? tenantId : "none",
                            clientIp);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}
