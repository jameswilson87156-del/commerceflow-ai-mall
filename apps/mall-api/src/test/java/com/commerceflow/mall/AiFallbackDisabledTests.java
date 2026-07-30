package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.commerceflow.mall.ai.AiModels;
import com.commerceflow.mall.ai.AiProviderClientException;
import com.commerceflow.mall.ai.AiService;
import com.commerceflow.mall.ai.CustomerServiceProviderClient;
import com.commerceflow.mall.core.CommerceException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "commerceflow.ai.fallback-enabled=false",
        "commerceflow.ai.rate-limit.enabled=false"
})
@AutoConfigureMockMvc
class AiFallbackDisabledTests {
    @Autowired AiService service;
    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerServiceProviderClient providerClient;

    @BeforeEach
    void providerFails() {
        reset(providerClient);
        when(providerClient.answer(any())).thenThrow(new AiProviderClientException(AiProviderClientException.Kind.UNAVAILABLE, "offline"));
    }

    @Test
    void fallbackFalseReturnsAServiceErrorInsteadOfALocalAnswer() {
        CommerceException exception = assertThrows(CommerceException.class,
                () -> service.ask(new AiModels.CustomerServiceAskRequest(1L, 101L, 10004L, "What is the SKU code?", "ai1-" + UUID.randomUUID())));
        assertEquals("AI_SERVICE_UNAVAILABLE", exception.code());
    }

    @Test
    void publicEndpointDoesNotPretendFallbackSucceededWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/ai/customer-service/ask")
                        .contentType("application/json")
                        .content("{\"userId\":1,\"productId\":101,\"skuId\":10004,\"question\":\"What is the SKU code?\",\"clientRequestId\":\"ai1-disabled-001\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"));
    }

    @Test
    void typedProviderFailureStillHasNoFallbackAnswerWhenDisabled() {
        reset(providerClient);
        when(providerClient.answer(any())).thenThrow(new AiProviderClientException(AiProviderClientException.Kind.FACT_MISMATCH, "mismatch"));
        CommerceException exception = assertThrows(CommerceException.class,
                () -> service.ask(new AiModels.CustomerServiceAskRequest(1L, 101L, 10004L, "What is the SKU code?", "ai1-" + UUID.randomUUID())));
        assertTrue(exception.getMessage().contains("unavailable"));
    }
}
