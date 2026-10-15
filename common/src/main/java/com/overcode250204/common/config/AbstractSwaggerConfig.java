package com.overcode250204.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

public abstract class AbstractSwaggerConfig {
    protected OpenAPI buildOpenAPI(String serviceName, String title, String version, String description, String gatewayUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title(title + " - " + serviceName)
                        .version(version)
                        .description(description)
                        .contact(new Contact()
                                .name("Overcode250204")
                                .email("khoinguyen.250204@gmail.com")
                                .url("khoinguyen.250204@gmail.com")))
                .servers(List.of(new Server().url(gatewayUrl + "/" + serviceName)))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
    protected GroupedOpenApi bizledgerGroup(String packageName) {
        return GroupedOpenApi.builder().group("API").packagesToScan("com.overcode250204." + packageName + ".controller").build();
    }
}
