package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.commerceflow.mall.ai.ratelimit.IdentityKeyResolver;
import com.commerceflow.mall.ai.ratelimit.RateLimitDecision;
import com.commerceflow.mall.ai.ratelimit.RateLimitUnavailableException;
import com.commerceflow.mall.ai.ratelimit.RateLimitProperties;
import com.commerceflow.mall.ai.ratelimit.RedisAiRateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RateLimitUnitTests {
    @Test
    void identityHashIsStableAndDoesNotExposeUserIdOrAddress() {
        RateLimitProperties properties = properties();
        IdentityKeyResolver resolver = new IdentityKeyResolver(properties);
        String first = resolver.identityHash(1L, "127.0.0.1");
        assertEquals(first, resolver.identityHash(1L, "127.0.0.1"));
        assertNotEquals(first, resolver.identityHash(2L, "127.0.0.1"));
        assertTrue(first.matches("[0-9a-f]{24}"));
        assertTrue(!first.contains("127") && !first.contains("1"));
    }

    @Test
    void disabledLimiterNeverAccessesRedis() {
        RateLimitProperties properties = properties();
        properties.setEnabled(false);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisAiRateLimiter limiter = new RedisAiRateLimiter(redis, properties, new IdentityKeyResolver(properties), Clock.systemUTC());
        assertEquals(RateLimitDecision.Mode.DISABLED, limiter.check(1L, "127.0.0.1").mode());
        verifyNoInteractions(redis);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void redisFailureUsesFailOpenWithoutInventingQuota() {
        RateLimitProperties properties = properties();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute((DefaultRedisScript) any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("offline"));
        RedisAiRateLimiter limiter = new RedisAiRateLimiter(redis, properties, new IdentityKeyResolver(properties), Clock.fixed(Instant.ofEpochSecond(120), ZoneOffset.UTC));
        RateLimitDecision decision = limiter.check(1L, "127.0.0.1");
        assertEquals(RateLimitDecision.Mode.DEGRADED, decision.mode());
        assertTrue(decision.allowed());
        assertEquals(null, decision.remaining());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void redisFailureHonorsFailClosedWhenExplicitlyConfigured() {
        RateLimitProperties properties = properties();
        properties.setFailurePolicy(RateLimitProperties.FailurePolicy.FAIL_CLOSED);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute((DefaultRedisScript) any(DefaultRedisScript.class), anyList(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("offline"));
        RedisAiRateLimiter limiter = new RedisAiRateLimiter(redis, properties, new IdentityKeyResolver(properties), Clock.systemUTC());
        assertThrows(RateLimitUnavailableException.class, () -> limiter.check(1L, "127.0.0.1"));
    }

    private RateLimitProperties properties() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setIdentityHashSecret("test-only-secret");
        properties.setKeyPrefix("commerceflow:test:ai:rate:v1");
        return properties;
    }
}
