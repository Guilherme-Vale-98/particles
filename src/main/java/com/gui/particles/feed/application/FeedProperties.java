package com.gui.particles.feed.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "particles.feed")
public record FeedProperties(
        @DefaultValue("" + DEFAULT_MAX_ITEMS_PER_USER) int maxItemsPerUser,
        @DefaultValue("7d") Duration feedTtl,
        @DefaultValue("" + DEFAULT_FANOUT_THRESHOLD) int fanoutThreshold
) {

    public static final int DEFAULT_MAX_ITEMS_PER_USER = 500;
    public static final Duration DEFAULT_FEED_TTL = Duration.ofDays(7);
    public static final int DEFAULT_FANOUT_THRESHOLD = 5_000;

    public FeedProperties {
        if (maxItemsPerUser < 1) {
            throw new IllegalArgumentException("maxItemsPerUser must be greater than zero");
        }
        if (feedTtl == null || feedTtl.isZero() || feedTtl.isNegative()) {
            throw new IllegalArgumentException("feedTtl must be greater than zero");
        }
        if (fanoutThreshold < 1) {
            throw new IllegalArgumentException("fanoutThreshold must be greater than zero");
        }
    }
}
