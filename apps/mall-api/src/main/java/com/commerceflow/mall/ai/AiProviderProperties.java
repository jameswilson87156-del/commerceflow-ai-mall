package com.commerceflow.mall.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime-only provider configuration. Secrets are injected through the
 * environment or a secret manager; this class deliberately has no toString
 * implementation so an API key cannot accidentally appear in diagnostics.
 */
@ConfigurationProperties("commerceflow.ai.provider")
public class AiProviderProperties {
    public static final String FASTAPI = "FASTAPI";
    public static final String OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";

    private String mode = FASTAPI;
    private String baseUrl = "http://127.0.0.1:8000";
    private String model = "";
    private String apiKey = "";
    private String path = "/v1/chat/completions";
    private int connectTimeoutMs = 1500;
    private int readTimeoutMs = 8000;
    private int maxRetries = 1;
    private boolean fallbackEnabled = true;
    private String providerName = "openai-compatible";

    public String getMode() { return mode; }
    public void setMode(String value) { mode = normalize(value, FASTAPI); }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { baseUrl = value == null ? "" : value.trim(); }
    public String getModel() { return model; }
    public void setModel(String value) { model = value == null ? "" : value.trim(); }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String value) { apiKey = value == null ? "" : value.trim(); }
    public String getPath() { return path; }
    public void setPath(String value) { path = value == null || value.isBlank() ? "/v1/chat/completions" : value.trim(); }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int value) { connectTimeoutMs = bounded(value, 100, 30_000, 1500); }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int value) { readTimeoutMs = bounded(value, 100, 120_000, 8000); }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int value) { maxRetries = bounded(value, 0, 3, 1); }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean value) { fallbackEnabled = value; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String value) { providerName = value == null || value.isBlank() ? "openai-compatible" : value.trim(); }

    public boolean isOpenAiCompatible() {
        return OPENAI_COMPATIBLE.equals(mode) || "OPENAI".equals(mode) || "DEEPSEEK".equals(mode);
    }

    /** Keeps the public runtime boundary compatible with the existing showcase contract. */
    public String runtimeMode() {
        return isOpenAiCompatible() ? AiModels.ProviderMode.REAL_OPENAI_COMPATIBLE.name() : AiModels.ProviderMode.MOCK.name();
    }

    private String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase().replace('-', '_');
        return normalized.isBlank() ? fallback : normalized;
    }

    private int bounded(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }
}
