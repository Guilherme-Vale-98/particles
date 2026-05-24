package com.gui.particles.article.application;

import com.gui.particles.article.domain.ArticleRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Component
public class ArticleViewCountFlusher {

    private static final String VIEW_COUNTER_KEY_PREFIX = "article:views:";
    private static final String VIEW_COUNTER_KEY_PATTERN = VIEW_COUNTER_KEY_PREFIX + "*";

    private final StringRedisTemplate redisTemplate;
    private final ArticleRepository articleRepository;

    public ArticleViewCountFlusher(StringRedisTemplate redisTemplate, ArticleRepository articleRepository) {
        this.redisTemplate = redisTemplate;
        this.articleRepository = articleRepository;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${particles.article.view-count-flush-delay-ms:60000}")
    public void flushViewCounts() {
        Set<String> keys = redisTemplate.keys(VIEW_COUNTER_KEY_PATTERN);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            flushKey(key);
        }
    }

    private void flushKey(String key) {
        UUID articleId = articleIdFromKey(key);
        if (articleId == null) {
            return;
        }

        Long delta = viewDelta(key);
        if (delta == null || delta <= 0) {
            return;
        }

        articleRepository.incrementViewCount(articleId, delta);
    }

    private UUID articleIdFromKey(String key) {
        if (key == null || !key.startsWith(VIEW_COUNTER_KEY_PREFIX)) {
            return null;
        }

        try {
            return UUID.fromString(key.substring(VIEW_COUNTER_KEY_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Long viewDelta(String key) {
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
