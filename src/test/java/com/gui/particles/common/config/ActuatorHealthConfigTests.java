package com.gui.particles.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorHealthConfigTests {

    private static final Path APPLICATION_YAML = Path.of("src/main/resources/application.yml");

    @Test
    void exposesHealthEndpointAndDetails() throws IOException {
        String config = Files.readString(APPLICATION_YAML);

        assertThat(config).contains("management:");
        assertThat(config).contains("web:");
        assertThat(config).contains("exposure:");
        assertThat(config).contains("include: health");
        assertThat(config).contains("show-details: always");
    }

    @Test
    void definesLivenessReadinessDatabaseAndCacheHealthGroups() throws IOException {
        String config = Files.readString(APPLICATION_YAML);

        assertThat(config).contains("liveness:");
        assertThat(config).contains("include: ping");
        assertThat(config).contains("readiness:");
        assertThat(config).contains("include: db,redis");
        assertThat(config).contains("database:");
        assertThat(config).contains("include: db");
        assertThat(config).contains("cache:");
        assertThat(config).contains("include: redis");
    }
}
