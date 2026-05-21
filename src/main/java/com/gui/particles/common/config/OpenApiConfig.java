package com.gui.particles.common.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI particlesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Particles API")
                        .version("0.0.1")
                        .description("Backend API for the Particles articles-based social network."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerJwtSecurityScheme()))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OperationCustomizer authenticationProviderHeaderCustomizer() {
        return (operation, handlerMethod) -> addAuthenticationProviderHeader(operation);
    }

    private SecurityScheme bearerJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    private Operation addAuthenticationProviderHeader(Operation operation) {
        return operation.addParametersItem(new Parameter()
                .name("type")
                .in("header")
                .required(false)
                .description("Authentication provider selector. Use credentials for the custom JWT flow, google for Google JWT, or github for GitHub opaque tokens. If omitted, credentials is used.")
                .schema(new StringSchema()
                        ._enum(java.util.List.of("credentials", "google", "github"))
                        ._default("credentials")));
    }
}
