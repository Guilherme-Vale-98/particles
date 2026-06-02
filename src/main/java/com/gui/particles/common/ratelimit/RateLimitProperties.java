package com.gui.particles.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "particles.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        long capacity,
        long refillTokens,
        Duration refillPeriod,
        String redisKeyPrefix
) {

    public RateLimitProperties {
        if (capacity < 1) {
            throw new IllegalArgumentException("Rate limit capacity must be positive");
        }
        if (refillTokens < 1) {
            throw new IllegalArgumentException("Rate limit refill tokens must be positive");
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("Rate limit refill period must be positive");
        }
        if (redisKeyPrefix == null || redisKeyPrefix.isBlank()) {
            redisKeyPrefix = "particles:rate-limit:";
        }
    }

    public BucketConfiguration bucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(refillTokens, refillPeriod)))
                .build();
    }

    public Duration stateTtl() {
        return refillPeriod.multipliedBy(2).plusSeconds(1);
    }
}
