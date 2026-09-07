package com.commerceflow.mall.core;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Rejects demo defaults and missing secrets whenever a deploy profile is selected. */
@Component
public class DeploymentSafetyValidator {
    private static final Set<String> EXTERNAL_PROVIDERS = Set.of("openai", "deepseek", "openai_compatible", "openai-compatible");

    private final Environment environment;

    public DeploymentSafetyValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateWhenDeploying() {
        if (!isDeploymentProfile()) return;

        requireEquals("commerceflow.auth.mode", "OIDC");
        requireNonBlank("commerceflow.auth.issuer-uri");
        requireEquals("commerceflow.showcase.authentication-mode", "EXTERNAL");
        requireEquals("commerceflow.showcase.operator-authentication-mode", "EXTERNAL");
        requireEquals("commerceflow.showcase.legacy-api-enabled", "false");
        requireEquals("commerceflow.ai.provider.fallback-enabled", "false");
        requireEquals("commerceflow.ai.rate-limit.enabled", "true");
        requireEquals("commerceflow.ai.rate-limit.failure-policy", "FAIL_CLOSED");

        requireNonBlank("spring.datasource.url");
        requireNonBlank("spring.datasource.username");
        requireNonBlank("spring.datasource.password");
        rejectDemoSecret("spring.datasource.password");
        requireNonBlank("commerceflow.ai.rate-limit.identity-hash-secret");
        rejectDemoSecret("commerceflow.ai.rate-limit.identity-hash-secret");

        String provider = value("commerceflow.ai.provider.mode").toLowerCase(Locale.ROOT);
        if (!EXTERNAL_PROVIDERS.contains(provider)) {
            throw new IllegalStateException("commerceflow.ai.provider.mode must select an external provider in staging/production.");
        }
        requireNonBlank("commerceflow.ai.provider.base-url");
        requireNonBlank("commerceflow.ai.provider.model");
        requireNonBlank("commerceflow.ai.provider.api-key");
        requireHttpsOrigins("commerceflow.cors.allowed-origins");
    }

    private boolean isDeploymentProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "staging".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private void requireHttpsOrigins(String key) {
        String origins = value(key);
        if (origins.isBlank()) throw new IllegalStateException(key + " must be configured in staging/production.");
        for (String origin : origins.split(",")) {
            if (!origin.trim().matches("https://[A-Za-z0-9.-]+(?::[0-9]+)?")) {
                throw new IllegalStateException(key + " must contain only HTTPS origins without paths in staging/production.");
            }
        }
    }

    private void rejectDemoSecret(String key) {
        String normalized = value(key).toLowerCase(Locale.ROOT);
        if (normalized.contains("commerceflow_demo") || normalized.contains("root_demo") || normalized.contains("local-showcase")) {
            throw new IllegalStateException(key + " must not use a demo secret in staging/production.");
        }
    }

    private void requireEquals(String key, String expected) {
        if (!expected.equalsIgnoreCase(value(key))) {
            throw new IllegalStateException(key + " must be " + expected + " in staging/production.");
        }
    }

    private void requireNonBlank(String key) {
        if (value(key).isBlank()) {
            throw new IllegalStateException(key + " must be configured in staging/production.");
        }
    }

    private String value(String key) {
        return environment.getProperty(key, "").trim();
    }
}
