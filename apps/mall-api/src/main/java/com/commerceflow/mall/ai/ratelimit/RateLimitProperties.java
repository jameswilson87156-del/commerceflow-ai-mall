package com.commerceflow.mall.ai.ratelimit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("commerceflow.ai.rate-limit")
public class RateLimitProperties {
    public enum FailurePolicy { FAIL_OPEN, FAIL_CLOSED }

    private boolean enabled = true;
    @Min(1) @Max(60) private int limit = 5;
    @Min(1) @Max(3600) private int windowSeconds = 60;
    @NotBlank @Pattern(regexp = "[A-Za-z0-9:_-]{1,80}")
    private String keyPrefix = "commerceflow:local:ai:rate:v1";
    private FailurePolicy failurePolicy = FailurePolicy.FAIL_OPEN;
    @NotBlank private String identityHashSecret = "local-showcase-rate-limit-salt";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public FailurePolicy getFailurePolicy() { return failurePolicy; }
    public void setFailurePolicy(FailurePolicy failurePolicy) { this.failurePolicy = failurePolicy; }
    public String getIdentityHashSecret() { return identityHashSecret; }
    public void setIdentityHashSecret(String identityHashSecret) { this.identityHashSecret = identityHashSecret; }
}
