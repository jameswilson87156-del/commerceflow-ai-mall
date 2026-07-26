package com.commerceflow.mall.ai;

public class AiProviderClientException extends RuntimeException {
    public enum Kind { TIMEOUT, UNAVAILABLE, INVALID_RESPONSE, PROVIDER_ERROR }

    private final Kind kind;

    public AiProviderClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public AiProviderClientException(Kind kind, String message) {
        this(kind, message, null);
    }

    public Kind kind() {
        return kind;
    }
}
