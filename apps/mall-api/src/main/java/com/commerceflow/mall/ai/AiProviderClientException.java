package com.commerceflow.mall.ai;

public class AiProviderClientException extends RuntimeException {
    /** Safe boundary where a provider failure was observed. Never expose transport details. */
    public enum FailureSource {
        JAVA_TO_FASTAPI,
        REMOTE_PROVIDER,
        PROVIDER_RESPONSE_VALIDATION,
        PROVIDER_CONFIGURATION
    }

    public enum Kind {
        TIMEOUT,
        UNAVAILABLE,
        INVALID_RESPONSE,
        PROVIDER_ERROR,
        CONFIGURATION,
        PROTOCOL_UNSUPPORTED,
        HTTP_ERROR,
        FACT_MISMATCH
    }

    private final Kind kind;
    private final FailureSource failureSource;
    private final String safeErrorCode;

    public AiProviderClientException(Kind kind, String message, Throwable cause) {
        this(kind, defaultSource(kind), defaultCode(kind), message, cause);
    }

    public AiProviderClientException(Kind kind, String message) {
        this(kind, defaultSource(kind), defaultCode(kind), message, null);
    }

    public AiProviderClientException(Kind kind, FailureSource failureSource, String safeErrorCode, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.failureSource = failureSource;
        this.safeErrorCode = safeErrorCode;
    }

    public Kind kind() {
        return kind;
    }

    public FailureSource failureSource() {
        return failureSource;
    }

    /** Stable, secret-safe code that may be persisted in ai_trace. */
    public String safeErrorCode() {
        return safeErrorCode;
    }

    private static FailureSource defaultSource(Kind kind) {
        return switch (kind) {
            case TIMEOUT, UNAVAILABLE -> FailureSource.JAVA_TO_FASTAPI;
            case CONFIGURATION, PROTOCOL_UNSUPPORTED -> FailureSource.PROVIDER_CONFIGURATION;
            case INVALID_RESPONSE, FACT_MISMATCH, PROVIDER_ERROR, HTTP_ERROR -> FailureSource.PROVIDER_RESPONSE_VALIDATION;
        };
    }

    private static String defaultCode(Kind kind) {
        return switch (kind) {
            case TIMEOUT -> "JAVA_FASTAPI_TIMEOUT";
            case UNAVAILABLE -> "JAVA_FASTAPI_UNAVAILABLE";
            case CONFIGURATION -> "PROVIDER_CONFIGURATION_ERROR";
            case PROTOCOL_UNSUPPORTED -> "PROVIDER_PROTOCOL_UNSUPPORTED";
            case FACT_MISMATCH -> "PROVIDER_FACT_MISMATCH";
            case INVALID_RESPONSE -> "PROVIDER_INVALID_RESPONSE";
            case HTTP_ERROR -> "REMOTE_PROVIDER_HTTP_ERROR";
            case PROVIDER_ERROR -> "PROVIDER_INVALID_RESPONSE";
        };
    }
}
