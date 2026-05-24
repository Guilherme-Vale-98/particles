package com.gui.particles.article.application;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleViewCounterTests {

    @Test
    void recordViewIncrementsArticleViewRedisCounter() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        UUID articleId = UUID.fromString("41b5a3e2-07f7-44c2-8ea7-026d29adcb10");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArticleViewCounter viewCounter = new ArticleViewCounter(redisTemplate);

        viewCounter.recordView(articleId);

        verify(valueOperations).increment("article:views:41b5a3e2-07f7-44c2-8ea7-026d29adcb10");
    }

    @Test
    void recordViewIsAsync() throws NoSuchMethodException {
        Method method = ArticleViewCounter.class.getMethod("recordView", UUID.class);

        assertThat(method.getAnnotation(Async.class)).isNotNull();
    }

    @Test
    void scheduledFlusherMovesRedisDeltasToPostgres() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        com.gui.particles.article.domain.ArticleRepository articleRepository =
                mock(com.gui.particles.article.domain.ArticleRepository.class);
        UUID articleId = UUID.fromString("41b5a3e2-07f7-44c2-8ea7-026d29adcb10");
        when(redisTemplate.keys("article:views:*"))
                .thenReturn(Set.of("article:views:41b5a3e2-07f7-44c2-8ea7-026d29adcb10"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("article:views:41b5a3e2-07f7-44c2-8ea7-026d29adcb10"))
                .thenReturn("12");
        ArticleViewCountFlusher flusher = new ArticleViewCountFlusher(redisTemplate, articleRepository);

        flusher.flushViewCounts();

        verify(articleRepository).incrementViewCount(articleId, 12);
    }

    @Test
    void scheduledFlusherIgnoresInvalidKeysAndValues() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        com.gui.particles.article.domain.ArticleRepository articleRepository =
                mock(com.gui.particles.article.domain.ArticleRepository.class);
        when(redisTemplate.keys("article:views:*"))
                .thenReturn(Set.of("article:views:not-a-uuid", "article:views:41b5a3e2-07f7-44c2-8ea7-026d29adcb10"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("article:views:not-a-uuid")).thenReturn("5");
        when(valueOperations.getAndDelete("article:views:41b5a3e2-07f7-44c2-8ea7-026d29adcb10")).thenReturn("not-a-number");
        ArticleViewCountFlusher flusher = new ArticleViewCountFlusher(redisTemplate, articleRepository);

        flusher.flushViewCounts();

        verify(articleRepository, never()).incrementViewCount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void flushViewCountsIsScheduled() throws NoSuchMethodException {
        Method method = ArticleViewCountFlusher.class.getMethod("flushViewCounts");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).isEqualTo("${particles.article.view-count-flush-delay-ms:60000}");
    }
}
