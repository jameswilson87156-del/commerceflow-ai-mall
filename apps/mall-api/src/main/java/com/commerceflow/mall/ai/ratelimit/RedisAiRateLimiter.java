package com.commerceflow.mall.ai.ratelimit;

import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisAiRateLimiter implements AiRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(RedisAiRateLimiter.class);
    private static final DefaultRedisScript<List> FIXED_WINDOW_SCRIPT = script();

    private final StringRedisTemplate redis;
    private final RateLimitProperties properties;
    private final IdentityKeyResolver identities;
    private final Clock clock;

    @Autowired
    public RedisAiRateLimiter(StringRedisTemplate redis, RateLimitProperties properties, IdentityKeyResolver identities) {
        this(redis, properties, identities, Clock.systemUTC());
    }

    public RedisAiRateLimiter(StringRedisTemplate redis, RateLimitProperties properties, IdentityKeyResolver identities, Clock clock) {
        this.redis = redis;
        this.properties = properties;
        this.identities = identities;
        this.clock = clock;
    }

    @Override
    public RateLimitDecision check(long userId, String remoteAddress) {
        if (!properties.isEnabled()) return RateLimitDecision.disabled();

        long now = clock.instant().getEpochSecond();
        long windowStart = now - Math.floorMod(now, properties.getWindowSeconds());
        int secondsToReset = Math.max(1, (int) (windowStart + properties.getWindowSeconds() - now));
        String key = properties.getKeyPrefix() + ":{" + identities.identityHash(userId, remoteAddress) + "}:" + windowStart;

        try {
            List<?> result = redis.execute(FIXED_WINDOW_SCRIPT, List.of(key), String.valueOf(properties.getLimit()), String.valueOf(secondsToReset));
            if (result == null || result.size() != 3) throw new IllegalStateException("Unexpected Redis rate-limit script result");
            int count = number(result.get(0));
            int ttl = number(result.get(1));
            int remaining = number(result.get(2));
            int retryAfter = Math.max(1, ttl);
            boolean allowed = count <= properties.getLimit();
            return new RateLimitDecision(RateLimitDecision.Mode.REDIS, allowed, properties.getLimit(), remaining, now + retryAfter, retryAfter);
        } catch (RuntimeException ex) {
            log.warn("AI rate-limit storage unavailable; applying configured policy {}", properties.getFailurePolicy());
            if (properties.getFailurePolicy() == RateLimitProperties.FailurePolicy.FAIL_OPEN) return RateLimitDecision.degraded();
            throw new RateLimitUnavailableException();
        }
    }

    private static DefaultRedisScript<List> script() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/ai-rate-limit.lua"));
        script.setResultType(List.class);
        return script;
    }

    private int number(Object value) {
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(value));
    }
}
