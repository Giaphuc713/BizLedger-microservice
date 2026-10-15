package com.overcode250204.apigatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class GatewayConfig {
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNullElse(
                        exchange.getRequest().getHeaders().getFirst("X-Tenant-Id"),
                        Objects.requireNonNullElse(
                                exchange.getRequest().getHeaders().getFirst("Authorization"),
                                Objects.requireNonNullElse(
                                        exchange.getRequest().getRemoteAddress(),
                                        "unknown").toString())));
    }
}
