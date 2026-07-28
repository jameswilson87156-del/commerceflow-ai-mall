package com.commerceflow.mall.ai.ratelimit;

public class RateLimitExceededException extends RuntimeException {
    private final RateLimitDecision decision;

    public RateLimitExceededException(RateLimitDecision decision) {
        super("请求过于频繁，请在 " + decision.retryAfterSeconds() + " 秒后重试。");
        this.decision = decision;
    }

    public RateLimitDecision decision() { return decision; }
}
