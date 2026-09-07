package com.commerceflow.mall.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AiProviderObservabilityTests {
    @Test
    void recordsOnlyLowCardinalityModeAndOutcomeDimensions() {
        var registry = new SimpleMeterRegistry();
        var properties = new AiProviderProperties();
        var metrics = new AiProviderMetrics(registry, properties);

        metrics.record("TIMEOUT", 12);
        metrics.record("question text must never be a tag", 7);

        assertEquals(1.0, registry.get("commerceflow.ai.provider.requests")
                .tag("mode", "MOCK").tag("outcome", "TIMEOUT").counter().count());
        assertEquals(1.0, registry.get("commerceflow.ai.provider.requests")
                .tag("mode", "MOCK").tag("outcome", "PROVIDER_ERROR").counter().count());
        assertEquals(2L, registry.get("commerceflow.ai.provider.latency")
                .tag("mode", "MOCK").timer().count());
        assertNotNull(registry.get("commerceflow.ai.provider.requests").tag("mode", "MOCK"));
    }
}
