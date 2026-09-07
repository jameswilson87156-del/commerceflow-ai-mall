package com.commerceflow.mall.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {
        "commerceflow.ai.provider.mode=OPENAI_COMPATIBLE",
        "commerceflow.ai.provider.base-url=http://127.0.0.1:1",
        "commerceflow.ai.provider.model=gpt-test",
        "commerceflow.ai.provider.api-key=provided-only-for-test",
        "commerceflow.ai.rate-limit.enabled=false"})
class AiExternalProviderContextTests {
    @Autowired ApplicationContext context;

    @Test
    void externalModeSelectsOnlyCompatibleAdapterAndKeepsOperationsModeHonest() {
        assertInstanceOf(OpenAiCompatibleCustomerServiceProviderClient.class,
                context.getBean(CustomerServiceProviderClient.class));
        assertEquals(AiModels.ProviderMode.REAL_OPENAI_COMPATIBLE.name(),
                context.getBean(AiProviderProperties.class).runtimeMode());
    }
}
