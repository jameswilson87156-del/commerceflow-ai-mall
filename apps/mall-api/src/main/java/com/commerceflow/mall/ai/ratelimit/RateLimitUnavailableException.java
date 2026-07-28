package com.commerceflow.mall.ai.ratelimit;

public class RateLimitUnavailableException extends RuntimeException {
    public RateLimitUnavailableException() {
        super("AI 请求保护暂时不可用，请稍后重试。");
    }
}
