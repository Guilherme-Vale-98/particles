package com.gui.particles.common.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTests {

    @Test
    void createsOpenApiMetadata() {
        OpenAPI openAPI = new OpenApiConfig().particlesOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Particles API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("0.0.1");
        assertThat(openAPI.getInfo().getDescription()).contains("articles-based social network");
    }

    @Test
    void documentsBearerJwtSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().particlesOpenApi();

        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getScheme()).isEqualTo("bearer");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity()).anySatisfy(requirement ->
                assertThat(requirement).containsKey("bearerAuth")
        );
    }

    @Test
    void documentsAuthenticationProviderTypeHeader() {
        Operation operation = new OpenApiConfig()
                .authenticationProviderHeaderCustomizer()
                .customize(new Operation(), null);

        assertThat(operation.getParameters()).anySatisfy(parameter -> {
            assertThat(parameter.getName()).isEqualTo("type");
            assertThat(parameter.getIn()).isEqualTo("header");
            assertThat(parameter.getRequired()).isFalse();
            assertThat(parameter.getDescription()).contains("credentials", "google", "github");
            assertThat(parameter.getSchema().getEnum()).containsExactly("credentials", "google", "github");
        });
    }
}
