package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.commerceflow.mall.ai.AiModels;
import com.commerceflow.mall.ai.AiProviderClientException;
import com.commerceflow.mall.ai.HttpCustomerServiceProviderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HttpCustomerServiceProviderClientTests {
    @Test
    void localJavaToFastApiConnectionFailureUsesDedicatedSafeCode() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        AiProviderClientException failure = assertThrows(AiProviderClientException.class,
                () -> client("http://127.0.0.1:" + unusedPort, 100).answer(request()));
        assertEquals(AiProviderClientException.FailureSource.JAVA_TO_FASTAPI, failure.failureSource());
        assertEquals("JAVA_FASTAPI_UNAVAILABLE", failure.safeErrorCode());
    }

    @Test
    void localJavaToFastApiTimeoutUsesDedicatedSafeCode() throws Exception {
        AiProviderClientException failure = new AiProviderClientException(AiProviderClientException.Kind.TIMEOUT,
                "local timeout stub", new SocketTimeoutException("local timeout stub"));
        assertEquals(AiProviderClientException.FailureSource.JAVA_TO_FASTAPI, failure.failureSource());
        assertEquals("JAVA_FASTAPI_TIMEOUT", failure.safeErrorCode());
    }

    @Test
    void fastApiTypedFailuresKeepRemoteOrValidationSourceWithoutLeakingBody() throws Exception {
        for (String code : List.of("REMOTE_PROVIDER_AUTH_REJECTED", "REMOTE_PROVIDER_NOT_FOUND",
                "REMOTE_PROVIDER_RATE_LIMITED", "REMOTE_PROVIDER_SERVER_ERROR")) {
            try (StubServer server = StubServer.responding(503, "{\"code\":\"" + code + "\",\"message\":\"sensitive-upstream-text\"}")) {
                AiProviderClientException failure = assertThrows(AiProviderClientException.class,
                        () -> client(server.baseUrl(), 200).answer(request()));
                assertEquals(AiProviderClientException.FailureSource.REMOTE_PROVIDER, failure.failureSource());
                assertEquals(code, failure.safeErrorCode());
                assertFalse(failure.getMessage().contains("sensitive-upstream-text"));
            }
        }
    }

    @Test
    void fastApiValidationFailuresKeepValidationSource() throws Exception {
        for (String code : List.of("PROVIDER_INVALID_JSON", "PROVIDER_FACT_MISMATCH")) {
            try (StubServer server = StubServer.responding(503, "{\"code\":\"" + code + "\",\"message\":\"sensitive-upstream-text\"}")) {
                AiProviderClientException failure = assertThrows(AiProviderClientException.class,
                        () -> client(server.baseUrl(), 200).answer(request()));
                assertEquals(AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, failure.failureSource());
                assertEquals(code, failure.safeErrorCode());
            }
        }
    }

    @Test
    void malformedFastApiFailureBodyFailsClosedWithoutPersistingIt() throws Exception {
        try (StubServer server = StubServer.responding(503, "not-json-sensitive-upstream-text")) {
            AiProviderClientException failure = assertThrows(AiProviderClientException.class,
                    () -> client(server.baseUrl(), 200).answer(request()));
            assertEquals(AiProviderClientException.FailureSource.PROVIDER_RESPONSE_VALIDATION, failure.failureSource());
            assertEquals("PROVIDER_INVALID_RESPONSE", failure.safeErrorCode());
            assertFalse(failure.getMessage().contains("sensitive-upstream-text"));
        }
    }

    @Test
    void scaledSlowLocalStubCompletesBeyondThePreviousReadBudget() throws Exception {
        // The 45 ms stub delay is deliberately beyond the scaled 30 ms legacy budget.
        try (StubServer server = StubServer.respondingAfter(45, 200, successfulResponse())) {
            var response = client(server.baseUrl(), 100).answer(request());
            assertEquals("ai11-stub-trace", response.traceId());
            assertEquals(AiModels.AnswerStatus.ANSWERED, response.answerStatus());
        }
    }

    @Test
    void localAndRemoteTimeoutCodesRemainDistinct() {
        AiProviderClientException local = new AiProviderClientException(AiProviderClientException.Kind.TIMEOUT,
                "local timeout", new SocketTimeoutException("local timeout"));
        AiProviderClientException remote = new AiProviderClientException(AiProviderClientException.Kind.TIMEOUT,
                AiProviderClientException.FailureSource.REMOTE_PROVIDER, "REMOTE_PROVIDER_TIMEOUT", "remote timeout", null);
        assertEquals("JAVA_FASTAPI_TIMEOUT", local.safeErrorCode());
        assertEquals("REMOTE_PROVIDER_TIMEOUT", remote.safeErrorCode());
    }

    private HttpCustomerServiceProviderClient client(String baseUrl, int timeoutMs) {
        return new HttpCustomerServiceProviderClient(new ObjectMapper().findAndRegisterModules(), baseUrl, timeoutMs, timeoutMs);
    }

    private AiModels.PythonCustomerServiceRequest request() {
        return new AiModels.PythonCustomerServiceRequest("ai11-stub-trace", "ai11-stub-client", "What is the SKU code?",
                new AiModels.BusinessFacts("What is the SKU code?", 101, "PROD-101", "Synthetic product", "ON_SALE", null,
                        10004, "SKU-10004", "Gray", "L", "ON_SALE", new BigDecimal("129.00"), "CNY", 27, List.of(), Instant.now()));
    }

    private String successfulResponse() {
        return "{\"traceId\":\"ai11-stub-trace\",\"answer\":\"grounded answer\",\"answerStatus\":\"ANSWERED\","
                + "\"provider\":{\"name\":\"openai-compatible\",\"mode\":\"REAL_OPENAI_COMPATIBLE\",\"model\":null},\"warning\":null}";
    }

    private static final class StubServer implements AutoCloseable {
        private final HttpServer server;

        private StubServer(HttpServer server) {
            this.server = server;
            server.start();
        }

        static StubServer responding(int status, String body) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/ai/customer-service/answer", exchange -> write(exchange, status, body));
            return new StubServer(server);
        }

        static StubServer respondingAfter(long delayMs, int status, String body) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/ai/customer-service/answer", exchange -> {
                try {
                    Thread.sleep(delayMs);
                    write(exchange, status, body);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    exchange.close();
                }
            });
            return new StubServer(server);
        }

        private static void write(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
