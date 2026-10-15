package com.overcode250204.orderservice.config.swagger;

import com.overcode250204.common.config.AbstractSwaggerConfig;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig extends AbstractSwaggerConfig {
    @Bean
    public OpenAPI openAPI(@Value("${spring.application.name}") String serviceName,
                           @Value("${swagger.title:API Documentation}") String title,
                           @Value("${swagger.version:1.0}") String version,
                           @Value("${swagger.description:API documentation}") String description, @Value("${swagger.gateway-url}") String gatewayUrl) {
        return buildOpenAPI(serviceName, title, version, description, gatewayUrl);
    }

    @Bean
    public GroupedOpenApi groupedOpenAPI(@Value("${swagger.package}") String packageName) {
        return bizledgerGroup(packageName);
    }
}
