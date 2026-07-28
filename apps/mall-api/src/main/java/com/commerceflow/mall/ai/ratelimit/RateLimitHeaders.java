package com.commerceflow.mall.ai.ratelimit;

import org.springframework.http.HttpHeaders;

public final class RateLimitHeaders {
    private RateLimitHeaders() {
    }

    public static HttpHeaders success(RateLimitDecision decision) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Mode", mode(decision));
        if (decision.mode() == RateLimitDecision.Mode.REDIS) applyQuota(headers, decision);
        return headers;
    }

    public static HttpHeaders rejected(RateLimitDecision decision) {
        HttpHeaders headers = success(decision);
        headers.set("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        return headers;
    }

    private static void applyQuota(HttpHeaders headers, RateLimitDecision decision) {
        headers.set("X-RateLimit-Limit", String.valueOf(decision.limit()));
        headers.set("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        headers.set("X-RateLimit-Reset", String.valueOf(decision.resetEpochSeconds()));
    }

    private static String mode(RateLimitDecision decision) {
        return switch (decision.mode()) {
            case REDIS -> "redis";
            case DEGRADED -> "degraded";
            case DISABLED -> "disabled";
        };
    }
}
