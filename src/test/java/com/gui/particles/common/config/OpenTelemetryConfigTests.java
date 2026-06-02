package com.gui.particles.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OpenTelemetryConfigTests {

    private static final Path APPLICATION_YAML = Path.of("src/main/resources/application.yml");

    @Test
    void configuresTracingSamplingAndOtlpExport() throws IOException {
        String config = Files.readString(APPLICATION_YAML);

        assertThat(config).contains("tracing:");
        assertThat(config).contains("sampling:");
        assertThat(config).contains("probability: ${OTEL_TRACES_SAMPLER_ARG:1.0}");
        assertThat(config).contains("otlp:");
        assertThat(config).contains("tracing:");
        assertThat(config).contains("endpoint: ${OTEL_EXPORTER_OTLP_TRACES_ENDPOINT:http://localhost:4318/v1/traces}");
    }

    @Test
    void configuresOpenTelemetryResourceAttributes() throws IOException {
        String config = Files.readString(APPLICATION_YAML);

        assertThat(config).contains("otel:");
        assertThat(config).contains("resource:");
        assertThat(config).contains("attributes:");
        assertThat(config).contains("service.name: ${OTEL_SERVICE_NAME:particles}");
        assertThat(config).contains("service.version: ${OTEL_SERVICE_VERSION:0.0.1-SNAPSHOT}");
    }
}
