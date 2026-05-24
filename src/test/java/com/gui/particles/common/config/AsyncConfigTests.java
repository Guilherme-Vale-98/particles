package com.gui.particles.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTests {

    @Test
    void enablesSpringAsyncMethods() {
        assertThat(AsyncConfig.class.getAnnotation(EnableAsync.class)).isNotNull();
    }

    @Test
    void enablesScheduledMethods() {
        assertThat(AsyncConfig.class.getAnnotation(EnableScheduling.class)).isNotNull();
    }
}
