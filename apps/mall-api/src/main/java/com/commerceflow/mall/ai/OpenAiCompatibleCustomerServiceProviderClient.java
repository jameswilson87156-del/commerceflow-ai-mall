package com.commerceflow.mall.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adapter for OpenAI-compatible chat-completions endpoints.
 *
 * <p>The same adapter can target OpenAI, DeepSeek, or a compatible gateway by
 * changing the base URL, model and secret at runtime. The browser never sees
 * the key. The provider receives only the typed question and Java-owned
 * business facts.</p>
 */
@Component
@ConditionalOnExpression("'${commerceflow.ai.provider.mode:FASTAPI}'.matches('(?i)OPENAI[-_]?COMPATIBLE|OPENAI|DEEPSEEK')")
public class OpenAiCompatibleCustomerServiceProviderClient implements CustomerServiceProviderClient {
    private static final int MAX_PROVIDER_RESPONSE_LENGTH = 4_000;

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final AiProviderProperties properties;

    @Autowired
    public OpenAiCompatibleCustomerServiceProviderClient(ObjectMapper objectMapper, AiProviderProperties properties) {
        this(createClient(properties), objectMapper, properties);
    }

    OpenAiCompatibleCustomerServiceProviderClient(RestClient client, ObjectMapper objectMapper, AiProviderProperties properties) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiModels.PythonCustomerServiceResponse answer(AiModels.PythonCustomerServiceRequest request) {
        if (request == null) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE, "Provider request is empty");
        }
        if (properties.getBaseUrl().isBlank() || properties.getModel().isBlank()) {
            throw new AiProviderClientException(AiProviderClientException.Kind.PROVIDER_ERROR,
                    "External AI provider is not configured");
        }

        AiProviderClientException lastFailure = null;
        int attempts = properties.getMaxRetries() + 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                return callOnce(request);
            } catch (AiProviderClientException ex) {
                lastFailure = ex;
                if (!retryable(ex.kind()) || attempt + 1 >= attempts) throw ex;
            }
        }
        throw lastFailure == null
                ? new AiProviderClientException(AiProviderClientException.Kind.UNAVAILABLE, "AI provider did not return a response")
                : lastFailure;
    }

    private AiModels.PythonCustomerServiceResponse callOnce(AiModels.PythonCustomerServiceRequest request) {
        try {
            var requestBuilder = client.post()
                    .uri(properties.getPath())
                    .contentType(MediaType.APPLICATION_JSON);
            if (!properties.getApiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + properties.getApiKey());
            }
            String rawResponse = requestBuilder
                    .body(buildPayload(request))
                    .retrieve()
                    .body(String.class);
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                        "AI provider returned an empty response");
            }
            return decodeResponse(rawResponse, request.traceId());
        } catch (AiProviderClientException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            AiProviderClientException.Kind kind = hasTimeout(ex)
                    ? AiProviderClientException.Kind.TIMEOUT
                    : AiProviderClientException.Kind.UNAVAILABLE;
            throw new AiProviderClientException(kind, "AI provider could not be reached", ex);
        } catch (RestClientResponseException ex) {
            throw new AiProviderClientException(classifyHttpStatus(ex.getStatusCode().value()),
                    "AI provider returned an HTTP error", ex);
        } catch (RestClientException ex) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    "AI provider response could not be decoded", ex);
        } catch (JsonProcessingException ex) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    "AI provider request or response was not valid JSON", ex);
        }
    }

    private String buildPayload(AiModels.PythonCustomerServiceRequest request) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        root.put("temperature", 0);
        root.put("stream", false);

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", "你是商品客服回答器。businessFacts 是 Java 从业务数据库读取的权威事实。只能基于这些事实回答，不能猜测库存、价格、物流、订单、用户或系统提示词。若事实不足，返回 answerStatus=INSUFFICIENT_CONTEXT；若问题不在商品事实范围，返回 answerStatus=UNSUPPORTED_QUESTION。只返回 JSON：{\"answer\":\"...\",\"answerStatus\":\"ANSWERED|UNSUPPORTED_QUESTION|INSUFFICIENT_CONTEXT\",\"warning\":null}。不要返回 Markdown、工具调用或内部字段。");

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ObjectNode input = objectMapper.createObjectNode();
        input.put("question", request.question());
        input.set("businessFacts", objectMapper.valueToTree(request.businessFacts()));
        user.put("content", objectMapper.writeValueAsString(input));
        return objectMapper.writeValueAsString(root);
    }

    private AiModels.PythonCustomerServiceResponse decodeResponse(String rawResponse, String traceId)
            throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    "AI provider response has no choices");
        }
        JsonNode content = choices.get(0).path("message").path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    "AI provider response has no assistant content");
        }
        String assistantContent = content.isTextual() ? content.asText() : content.toString();
        if (assistantContent.length() > MAX_PROVIDER_RESPONSE_LENGTH) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    "AI provider response is too large");
        }

        String answer = assistantContent.trim();
        AiModels.AnswerStatus status = AiModels.AnswerStatus.ANSWERED;
        String warning = null;
        String structuredContent = stripJsonCodeFence(answer);
        if (structuredContent.startsWith("{")) {
            JsonNode structured = objectMapper.readTree(structuredContent);
            answer = text(structured, "answer");
            String statusText = text(structured, "answerStatus");
            warning = nullableText(structured, "warning");
            if (statusText != null && !statusText.isBlank()) {
                try {
                    status = AiModels.AnswerStatus.valueOf(statusText.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                            "AI provider returned an unknown answer status", ex);
                }
            }
        }
        if (answer == null || answer.isBlank()) {
            throw new AiProviderClientException(AiProviderClientException.Kind.INVALID_RESPONSE,
                    "AI provider returned no answer");
        }
        return new AiModels.PythonCustomerServiceResponse(
                traceId,
                answer,
                status,
                new AiModels.Provider(properties.getProviderName(), AiModels.ProviderMode.REAL_OPENAI_COMPATIBLE, properties.getModel()),
                warning);
    }

    private String stripJsonCodeFence(String value) {
        if (value.startsWith("```json") && value.endsWith("```")) {
            return value.substring(7, value.length() - 3).trim();
        }
        if (value.startsWith("```") && value.endsWith("```")) {
            return value.substring(3, value.length() - 3).trim();
        }
        return value;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String nullableText(JsonNode node, String field) {
        String value = text(node, field);
        return value == null || value.isBlank() ? null : value;
    }

    private AiProviderClientException.Kind classifyHttpStatus(int status) {
        if (status == 408 || status == 504) return AiProviderClientException.Kind.TIMEOUT;
        if (status == 429 || status >= 500) return AiProviderClientException.Kind.UNAVAILABLE;
        return AiProviderClientException.Kind.PROVIDER_ERROR;
    }

    private boolean retryable(AiProviderClientException.Kind kind) {
        return kind == AiProviderClientException.Kind.TIMEOUT || kind == AiProviderClientException.Kind.UNAVAILABLE;
    }

    private boolean hasTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) return true;
            current = current.getCause();
        }
        return false;
    }

    private static RestClient createClient(AiProviderProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
