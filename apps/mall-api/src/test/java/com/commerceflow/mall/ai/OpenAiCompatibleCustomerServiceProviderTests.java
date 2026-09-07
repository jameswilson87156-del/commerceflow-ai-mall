package com.commerceflow.mall.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleCustomerServiceProviderTests {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void mapsCompatibleResponseAndKeepsSecretOutOfRequestBody() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"answer\\\":\\\"灰色 L 码当前库存为 28 件。\\\",\\\"answerStatus\\\":\\\"ANSWERED\\\",\\\"warning\\\":null}\"}}]}");
        });

        AiProviderProperties properties = properties("deepseek-chat", "unit-test-secret");
        var response = new OpenAiCompatibleCustomerServiceProviderClient(
                new ObjectMapper().findAndRegisterModules(), properties).answer(request());

        assertEquals("trace-unit-1", response.traceId());
        assertEquals("灰色 L 码当前库存为 28 件。", response.answer());
        assertEquals(AiModels.AnswerStatus.ANSWERED, response.answerStatus());
        assertEquals(AiModels.ProviderMode.REAL_OPENAI_COMPATIBLE, response.provider().mode());
        assertEquals("openai-compatible", response.provider().name());
        assertEquals("Bearer unit-test-secret", authorization.get());
        assertTrue(requestBody.get().contains("businessFacts"));
        assertTrue(requestBody.get().contains("deepseek-chat"));
        assertFalse(requestBody.get().contains("unit-test-secret"));
    }

    @Test
    void retriesOnlyBoundedTransientFailuresAndClassifiesHttpErrors() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 429, "{\"error\":{\"message\":\"rate limited\"}}");
        });

        AiProviderProperties properties = properties("gpt-test", "");
        properties.setMaxRetries(1);
        var exception = assertThrows(AiProviderClientException.class, () ->
                new OpenAiCompatibleCustomerServiceProviderClient(new ObjectMapper().findAndRegisterModules(), properties)
                        .answer(request()));

        assertEquals(AiProviderClientException.Kind.UNAVAILABLE, exception.kind());
        assertEquals(2, requests.get());
    }

    @Test
    void doesNotRetryAuthenticationFailureAndRejectsMalformedProviderJson() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 401, "{\"error\":{\"message\":\"unauthorized\"}}");
        });
        AiProviderProperties properties = properties("gpt-test", "");
        properties.setMaxRetries(3);
        var unauthorized = assertThrows(AiProviderClientException.class, () ->
                new OpenAiCompatibleCustomerServiceProviderClient(new ObjectMapper().findAndRegisterModules(), properties)
                        .answer(request()));
        assertEquals(AiProviderClientException.Kind.PROVIDER_ERROR, unauthorized.kind());
        assertEquals(1, requests.get());

        server.stop(0);
        startServer(exchange -> respond(exchange, 200, "not-json"));
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        var malformed = assertThrows(AiProviderClientException.class, () ->
                new OpenAiCompatibleCustomerServiceProviderClient(new ObjectMapper().findAndRegisterModules(), properties)
                        .answer(request()));
        assertEquals(AiProviderClientException.Kind.INVALID_RESPONSE, malformed.kind());
    }

    private AiProviderProperties properties(String model, String apiKey) {
        var properties = new AiProviderProperties();
        properties.setMode("OPENAI_COMPATIBLE");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setModel(model);
        properties.setApiKey(apiKey);
        properties.setMaxRetries(0);
        return properties;
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", handler);
        server.start();
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (var output = exchange.getResponseBody()) {
            output.write(payload);
        }
    }

    private AiModels.PythonCustomerServiceRequest request() {
        return new AiModels.PythonCustomerServiceRequest(
                "trace-unit-1",
                "client-unit-1",
                "灰色 L 码还有库存吗？",
                new AiModels.BusinessFacts(
                        "灰色 L 码还有库存吗？", 101L, "PROD-1001", "基础款 T 恤", "ON_SALE", "/assets/tshirt.png",
                        10004L, "T-SHIRT-GRAY-L", "灰色", "L", "ON_SALE", java.math.BigDecimal.valueOf(129), "CNY", 28,
                        List.of(), java.time.Instant.now()));
    }
}
