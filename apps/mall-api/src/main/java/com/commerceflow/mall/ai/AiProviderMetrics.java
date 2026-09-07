package com.commerceflow.mall.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Low-cardinality Provider metrics. Only fixed mode/outcome values are used as
 * tags; question text, answer text, trace ids and secrets never enter a tag.
 */
@Component
public class AiProviderMetrics {
    private static final Set<String> OUTCOMES = Set.of(
            "SUCCESS", "PROVIDER_ERROR", "TIMEOUT", "UNAVAILABLE", "INVALID_RESPONSE");

    private final MeterRegistry registry;
    private final AiProviderProperties properties;

    public AiProviderMetrics(MeterRegistry registry, AiProviderProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    public void record(String outcome, long durationMs) {
        String safeOutcome = normalizeOutcome(outcome);
        String mode = properties.runtimeMode();
        Counter.builder("commerceflow.ai.provider.requests")
                .description("AI Provider calls classified by configured mode and safe outcome")
                .tag("mode", mode)
                .tag("outcome", safeOutcome)
                .register(registry)
                .increment();
        Timer.builder("commerceflow.ai.provider.latency")
                .description("AI Provider call duration")
                .tag("mode", mode)
                .register(registry)
                .record(Math.max(0, durationMs), TimeUnit.MILLISECONDS);
    }

    private String normalizeOutcome(String outcome) {
        String normalized = outcome == null ? "" : outcome.trim().toUpperCase(Locale.ROOT);
        return OUTCOMES.contains(normalized) ? normalized : "PROVIDER_ERROR";
    }
}
