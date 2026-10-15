package com.overcode250204.apigatewayservice.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class PublicUrlFilter implements GlobalFilter, Ordered {
    private static final List<String> PUBLIC_PATHS = List.of(
            "/swagger-ui",
            "/swagger-resources",
            "/webjars",
            "/configuration",
            "/identity-service/auth"

    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith) || path.endsWith("privileges") || path.endsWith("/v3/api-docs") || path.endsWith("google");

        if (isPublic) {
            exchange.getAttributes().put("isPublicRoute", true);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
