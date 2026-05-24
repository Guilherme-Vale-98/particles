package com.gui.particles.article.application;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class ArticleViewCounter {

    private static final String VIEW_COUNTER_KEY_PREFIX = "article:views:";

    private final StringRedisTemplate redisTemplate;

    public ArticleViewCounter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Async
    public void recordView(UUID articleId) {
        redisTemplate.opsForValue().increment(viewCounterKey(articleId));
    }

    private String viewCounterKey(UUID articleId) {
        return VIEW_COUNTER_KEY_PREFIX + Objects.requireNonNull(articleId, "articleId must not be null");
    }
}
