package com.gui.particles.common.ratelimit;

public interface RateLimiter {

    RateLimitDecision tryConsume(String key);
}
