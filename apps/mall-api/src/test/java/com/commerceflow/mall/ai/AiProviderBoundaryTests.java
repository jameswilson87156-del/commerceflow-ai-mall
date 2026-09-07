package com.commerceflow.mall.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.commerceflow.mall.ai.application.port.out.CustomerServiceProvider;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class AiProviderBoundaryTests {
    @Test
    void legacyClientNameIsOnlyAnOutboundPortCompatibilityAlias() {
        assertTrue(CustomerServiceProvider.class.isAssignableFrom(CustomerServiceProviderClient.class));
        assertTrue(CustomerServiceProvider.class.isAssignableFrom(HttpCustomerServiceProviderClient.class));
        assertTrue(CustomerServiceProvider.class.isAssignableFrom(OpenAiCompatibleCustomerServiceProviderClient.class));
    }

    @Test
    void aiServiceStoresTheProviderAsTheNewPort() throws Exception {
        Field provider = AiService.class.getDeclaredField("provider");
        assertEquals(CustomerServiceProvider.class, provider.getType());
        assertTrue(AiService.class.getDeclaredFields().length > 0);
    }

    @Test
    void externalConfigurationDefaultsToLocalFastApiAndKeepsSecretEmpty() {
        var properties = new AiProviderProperties();
        assertEquals(AiProviderProperties.FASTAPI, properties.getMode());
        assertFalse(properties.isOpenAiCompatible());
        assertEquals(AiModels.ProviderMode.MOCK.name(), properties.runtimeMode());
        assertEquals("", properties.getApiKey());
        assertEquals(1, properties.getMaxRetries());
        assertTrue(properties.isFallbackEnabled());
    }

    @Test
    void providerModeAcceptsDeepSeekAndOpenAiCompatibleAliases() {
        var properties = new AiProviderProperties();
        properties.setMode("deepseek");
        assertTrue(properties.isOpenAiCompatible());
        properties.setMode("openai-compatible");
        assertTrue(properties.isOpenAiCompatible());
        assertEquals(AiModels.ProviderMode.REAL_OPENAI_COMPATIBLE.name(), properties.runtimeMode());
    }
}
