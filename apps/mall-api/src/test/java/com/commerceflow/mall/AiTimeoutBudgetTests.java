package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class AiTimeoutBudgetTests {
    private static final int PYTHON_REMOTE_READ_TIMEOUT_MS = 10_000;
    private static final int LOCAL_PROCESSING_HEADROOM_MS = 5_000;

    @Test
    void javaReadBudgetExceedsPythonRemoteReadBudgetWithLocalHeadroom() {
        String configured = applicationYaml();
        Matcher matcher = Pattern.compile("AI_READ_TIMEOUT_MS:(\\d+)").matcher(configured);
        assertTrue(matcher.find(), "application.yml must provide a bounded default read timeout");
        int javaReadTimeoutMs = Integer.parseInt(matcher.group(1));
        assertTrue(javaReadTimeoutMs > PYTHON_REMOTE_READ_TIMEOUT_MS);
        assertTrue(javaReadTimeoutMs >= PYTHON_REMOTE_READ_TIMEOUT_MS + LOCAL_PROCESSING_HEADROOM_MS);
    }

    @Test
    void javaFallbackPropertyUsesProjectSharedLegacyThenSafeDefaultOrder() {
        String configured = applicationYaml();
        assertTrue(configured.contains("COMMERCEFLOW_AI_FALLBACK_ENABLED:${PORTFOLIO_AI_FALLBACK_ENABLED:${AI_PROVIDER_FALLBACK_ENABLED:true}}"));
    }

    private String applicationYaml() {
        try {
            return Files.readString(Path.of("src", "main", "resources", "application.yml"), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new AssertionError("application.yml must be available to the timeout-budget test", exception);
        }
    }
}
