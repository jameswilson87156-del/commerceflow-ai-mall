package com.commerceflow.mall.ai.ratelimit;

public record RateLimitDecision(
        Mode mode,
        boolean allowed,
        Integer limit,
        Integer remaining,
        Long resetEpochSeconds,
        Integer retryAfterSeconds) {
    public enum Mode { REDIS, DEGRADED, DISABLED }

    public static RateLimitDecision disabled() {
        return new RateLimitDecision(Mode.DISABLED, true, null, null, null, null);
    }

    public static RateLimitDecision degraded() {
        return new RateLimitDecision(Mode.DEGRADED, true, null, null, null, null);
    }
}
