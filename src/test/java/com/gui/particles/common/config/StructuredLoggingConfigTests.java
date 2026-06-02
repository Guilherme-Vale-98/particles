package com.gui.particles.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredLoggingConfigTests {

    private static final Path LOGBACK_CONFIG = Path.of("src/main/resources/logback-spring.xml");

    @Test
    void configuresJsonConsoleLogging() throws IOException {
        String config = Files.readString(LOGBACK_CONFIG);

        assertThat(config).contains("net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder");
        assertThat(config).contains("<timestamp>");
        assertThat(config).contains("<logLevel>");
        assertThat(config).contains("<loggerName>");
        assertThat(config).contains("<threadName>");
        assertThat(config).contains("<message>");
    }

    @Test
    void includesApplicationAndTracingFields() throws IOException {
        String config = Files.readString(LOGBACK_CONFIG);

        assertThat(config).contains("<springProperty");
        assertThat(config).contains("source=\"spring.application.name\"");
        assertThat(config).contains("\"application\": \"${applicationName}\"");
        assertThat(config).contains("\"traceId\": \"%X{traceId:-}\"");
        assertThat(config).contains("\"spanId\": \"%X{spanId:-}\"");
    }
}
