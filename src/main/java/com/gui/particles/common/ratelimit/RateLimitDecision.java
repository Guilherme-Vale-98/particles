package com.gui.particles.common.ratelimit;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, long remainingTokens, Duration retryAfter) {

    public static RateLimitDecision allowed(long remainingTokens) {
        return new RateLimitDecision(true, remainingTokens, Duration.ZERO);
    }

    public static RateLimitDecision rejected(Duration retryAfter) {
        return new RateLimitDecision(false, 0, retryAfter);
    }
}
