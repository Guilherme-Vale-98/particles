package com.gui.particles.feed.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedPropertiesTests {

    @Test
    void providesUsefulDefaults() {
        FeedProperties properties = new FeedProperties(
                FeedProperties.DEFAULT_MAX_ITEMS_PER_USER,
                FeedProperties.DEFAULT_FEED_TTL,
                FeedProperties.DEFAULT_FANOUT_THRESHOLD
        );

        assertThat(properties.maxItemsPerUser()).isEqualTo(500);
        assertThat(properties.feedTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.fanoutThreshold()).isEqualTo(5_000);
    }

    @Test
    void rejectsNonPositiveValues() {
        assertThatThrownBy(() -> new FeedProperties(0, Duration.ofDays(7), 5_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxItemsPerUser must be greater than zero");
        assertThatThrownBy(() -> new FeedProperties(500, Duration.ZERO, 5_000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("feedTtl must be greater than zero");
        assertThatThrownBy(() -> new FeedProperties(500, Duration.ofDays(7), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("fanoutThreshold must be greater than zero");
    }

    @Test
    void bindsFromParticlesFeedProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class)
                .withPropertyValues(
                        "particles.feed.max-items-per-user=25",
                        "particles.feed.feed-ttl=2h",
                        "particles.feed.fanout-threshold=100"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FeedProperties.class);
                    FeedProperties properties = context.getBean(FeedProperties.class);
                    assertThat(properties.maxItemsPerUser()).isEqualTo(25);
                    assertThat(properties.feedTtl()).isEqualTo(Duration.ofHours(2));
                    assertThat(properties.fanoutThreshold()).isEqualTo(100);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FeedProperties.class)
    private static class TestConfiguration {
    }
}
