package com.gui.particles.common.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AbstractCompareAndSwapBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.AsyncCompareAndSwapOperation;
import io.github.bucket4j.distributed.proxy.generic.compare_and_swap.CompareAndSwapOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class RedisBucket4jRateLimiter implements RateLimiter {

    private final RateLimitProperties properties;
    private final RedisProxyManager proxyManager;

    public RedisBucket4jRateLimiter(RateLimitProperties properties, RedisTemplate<String, byte[]> redisTemplate) {
        this.properties = properties;
        this.proxyManager = new RedisProxyManager(redisTemplate, properties.stateTtl());
    }

    @Override
    public RateLimitDecision tryConsume(String key) {
        Bucket bucket = proxyManager.builder()
                .build(properties.redisKeyPrefix() + key, properties.bucketConfiguration());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitDecision.allowed(probe.getRemainingTokens());
        }
        return RateLimitDecision.rejected(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    private static final class RedisProxyManager extends AbstractCompareAndSwapBasedProxyManager<String> {

        private final RedisTemplate<String, byte[]> redisTemplate;
        private final Duration ttl;

        private RedisProxyManager(RedisTemplate<String, byte[]> redisTemplate, Duration ttl) {
            super(ClientSideConfig.getDefault());
            this.redisTemplate = redisTemplate;
            this.ttl = ttl;
        }

        @Override
        protected CompareAndSwapOperation beginCompareAndSwapOperation(String key) {
            return new RedisCompareAndSwapOperation(redisTemplate, key, ttl);
        }

        @Override
        protected AsyncCompareAndSwapOperation beginAsyncCompareAndSwapOperation(String key) {
            throw new UnsupportedOperationException("Async rate limiting is not supported");
        }

        @Override
        public boolean isAsyncModeSupported() {
            return false;
        }

        @Override
        public void removeProxy(String key) {
            redisTemplate.delete(key);
        }

        @Override
        protected CompletableFuture<Void> removeAsync(String key) {
            redisTemplate.delete(key);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RedisCompareAndSwapOperation implements CompareAndSwapOperation {

        private final RedisTemplate<String, byte[]> redisTemplate;
        private final String key;
        private final Duration ttl;

        private RedisCompareAndSwapOperation(RedisTemplate<String, byte[]> redisTemplate, String key, Duration ttl) {
            this.redisTemplate = redisTemplate;
            this.key = key;
            this.ttl = ttl;
        }

        @Override
        public Optional<byte[]> getStateData() {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        }

        @Override
        public boolean compareAndSwap(byte[] originalData, byte[] newData, RemoteBucketState newState) {
            return Boolean.TRUE.equals(redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings({"unchecked", "NullableProblems"})
                public Boolean execute(org.springframework.data.redis.core.RedisOperations operations) {
                    operations.watch(key);
                    ValueOperations<String, byte[]> values = operations.opsForValue();
                    byte[] currentData = values.get(key);
                    if (!Arrays.equals(originalData, currentData)) {
                        operations.unwatch();
                        return false;
                    }

                    operations.multi();
                    values.set(key, newData, ttl);
                    List<Object> results = operations.exec();
                    return results != null;
                }
            }));
        }
    }

    @Configuration
    static class RedisRateLimitConfig {

        @Bean
        RedisTemplate<String, byte[]> rateLimitRedisTemplate(
                org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory
        ) {
            RedisTemplate<String, byte[]> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(RedisSerializer.byteArray());
            template.setHashValueSerializer(RedisSerializer.byteArray());
            template.afterPropertiesSet();
            return template;
        }
    }
}
