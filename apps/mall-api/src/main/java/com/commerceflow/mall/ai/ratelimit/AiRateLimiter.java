package com.commerceflow.mall.ai.ratelimit;

public interface AiRateLimiter {
    RateLimitDecision check(long userId, String remoteAddress);
}
