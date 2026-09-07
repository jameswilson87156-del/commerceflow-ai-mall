package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerceflow.mall.ai.AiModels;
import com.commerceflow.mall.ai.AiProviderClientException;
import com.commerceflow.mall.ai.CustomerServiceProviderClient;
import com.commerceflow.mall.ai.ratelimit.AiRateLimiter;
import com.commerceflow.mall.ai.ratelimit.RateLimitDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6380",
        "commerceflow.ai.rate-limit.enabled=true",
        "commerceflow.ai.rate-limit.limit=5",
        "commerceflow.ai.rate-limit.window-seconds=60",
        "commerceflow.ai.rate-limit.failure-policy=FAIL_OPEN"})
@AutoConfigureMockMvc
class AiRateLimitRedisIntegrationTests {
    private static final String TEST_KEY_PREFIX =
            "commerceflow:test:ai:rate:v1-" + UUID.randomUUID().toString().replace("-", "");

    @Autowired MockMvc mockMvc;
    @Autowired StringRedisTemplate redis;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired AiRateLimiter limiter;
    @MockitoBean CustomerServiceProviderClient providerClient;
    private boolean redisAvailable;

    @DynamicPropertySource
    static void registerIsolatedRedisPrefix(DynamicPropertyRegistry registry) {
        registry.add("commerceflow.ai.rate-limit.key-prefix", () -> TEST_KEY_PREFIX);
    }

    private void requireLocalRedis() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 6380), 250);
            redisAvailable = true;
        } catch (IOException unavailable) {
            redisAvailable = false;
            Assumptions.assumeTrue(false, "Redis integration tests require Redis at 127.0.0.1:6380; start docker compose before running them.");
        }
    }

    @BeforeEach
    void resetRedisAndProvider() {
        requireLocalRedis();
        deleteTestKeys();
        jdbc.update("DELETE FROM ai_trace");
        reset(providerClient);
        when(providerClient.answer(any())).thenAnswer(invocation -> {
            AiModels.PythonCustomerServiceRequest request = invocation.getArgument(0);
            return new AiModels.PythonCustomerServiceResponse(
                    request.traceId(), "库存事实已由 Java 提供。", AiModels.AnswerStatus.ANSWERED,
                    new AiModels.Provider("commerceflow-mock", AiModels.ProviderMode.MOCK, null), null);
        });
    }

    @org.junit.jupiter.api.AfterEach
    void cleanUpTestKeys() {
        if (redisAvailable) {
            deleteTestKeys();
        }
    }

    private void deleteTestKeys() {
        Set<String> keys = redis.keys(TEST_KEY_PREFIX + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Test
    void allowsFiveThenReturns429WithHeadersWithoutCallingProviderAgain() throws Exception {
        for (int index = 1; index <= 5; index++) {
            mockMvc.perform(ask("127.0.0.51", 101, 10004, "request-" + index)
                    .header("Origin", "http://127.0.0.1:5173"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "5"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(5 - index)))
                    .andExpect(header().string("X-RateLimit-Mode", "redis"))
                    .andExpect(header().string("Access-Control-Expose-Headers", org.hamcrest.Matchers.containsString("X-RateLimit-Mode")));
        }

        var blocked = mockMvc.perform(ask("127.0.0.51", 101, 10004, "request-6"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.code").value("AI_RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.matchesPattern("请求过于频繁，请在 [1-9][0-9]* 秒后重试。")))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber())
                .andReturn();
        String retryAfter = blocked.getResponse().getHeader("Retry-After");
        assertTrue(blocked.getResponse().getContentAsString().contains("\"retryAfterSeconds\":" + retryAfter));
        verify(providerClient, org.mockito.Mockito.times(5)).answer(any());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM ai_trace", Integer.class));
    }

    @Test
    void keepsOneTtlAndSharesQuotaAcrossSkuButSeparatesRemoteIdentities() throws Exception {
        mockMvc.perform(ask("127.0.0.61", 101, 10004, "first")).andExpect(status().isOk());
        String key = redis.keys(TEST_KEY_PREFIX + ":*").iterator().next();
        long firstTtl = redis.getExpire(key, TimeUnit.SECONDS);
        mockMvc.perform(ask("127.0.0.61", 102, 10003, "other-sku")).andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "3"));
        long secondTtl = redis.getExpire(key, TimeUnit.SECONDS);
        assertTrue(firstTtl > 0);
        assertTrue(secondTtl > 0 && secondTtl <= firstTtl);
        assertFalse(key.contains("127.0.0.61"));
        assertFalse(key.contains("other-sku"));
        mockMvc.perform(ask("127.0.0.62", 101, 10004, "other-identity"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    void invalidInputDoesNotConsumeButValidMissingProductDoesConsume() throws Exception {
        mockMvc.perform(askWithQuestion("127.0.0.71", 101, 10004, "   ", "invalid-question"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
        assertEquals(0, redis.keys(TEST_KEY_PREFIX + ":*").size());

        mockMvc.perform(ask("127.0.0.71", 999, 10004, "missing-product"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(providerClient);
        for (int index = 0; index < 4; index++) {
            mockMvc.perform(ask("127.0.0.71", 101, 10004, "valid-" + index)).andExpect(status().isOk());
        }
        mockMvc.perform(ask("127.0.0.71", 101, 10004, "blocked-after-missing"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void unsupportedAndProviderFallbackRequestsStillConsumeRedisQuota() throws Exception {
        reset(providerClient);
        when(providerClient.answer(any())).thenAnswer(invocation -> {
            AiModels.PythonCustomerServiceRequest request = invocation.getArgument(0);
            return new AiModels.PythonCustomerServiceResponse(
                    request.traceId(), "unsupported", AiModels.AnswerStatus.UNSUPPORTED_QUESTION,
                    new AiModels.Provider("commerceflow-mock", AiModels.ProviderMode.MOCK, null), null);
        });
        mockMvc.perform(ask("127.0.0.91", 101, 10004, "unsupported"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "4"))
                .andExpect(jsonPath("$.answerStatus").value("UNSUPPORTED_QUESTION"));

        reset(providerClient);
        when(providerClient.answer(any())).thenThrow(new AiProviderClientException(AiProviderClientException.Kind.UNAVAILABLE, "offline"));
        mockMvc.perform(ask("127.0.0.91", 101, 10004, "provider-fallback"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "3"))
                .andExpect(jsonPath("$.answerStatus").value("FALLBACK_ANSWER"))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    @Test
    void concurrentChecksNeverAllowMoreThanConfiguredLimit() throws Exception {
        var pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<RateLimitDecision>> tasks = new ArrayList<>();
            for (int index = 0; index < 12; index++) tasks.add(() -> limiter.check(1L, "127.0.0.81"));
            long allowed = pool.invokeAll(tasks).stream().map(future -> {
                try { return future.get(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }).filter(RateLimitDecision::allowed).count();
            assertEquals(5, allowed);
        } finally {
            pool.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder ask(
            String remoteAddress, long productId, long skuId, String suffix) {
        return askWithQuestion(remoteAddress, productId, skuId, "库存还有吗？", suffix);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder askWithQuestion(
            String remoteAddress, long productId, long skuId, String question, String suffix) {
        String body = "{\"userId\":1,\"productId\":" + productId + ",\"skuId\":" + skuId
                + ",\"question\":\"" + question + "\",\"clientRequestId\":\"p5-" + suffix + "\"}";
        return post("/api/ai/customer-service/ask").contentType("application/json").content(body)
                .with(request -> { request.setRemoteAddr(remoteAddress); return request; });
    }
}
