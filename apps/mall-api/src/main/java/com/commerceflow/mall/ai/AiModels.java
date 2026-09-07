package com.commerceflow.mall.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AiModels {
    private AiModels() {
    }

    public enum AnswerStatus {
        ANSWERED,
        UNSUPPORTED_QUESTION,
        INSUFFICIENT_CONTEXT,
        PROVIDER_ERROR,
        FALLBACK_ANSWER
    }

    public enum ProviderMode {
        MOCK,
        REAL_OPENAI_COMPATIBLE,
        FALLBACK
    }

    public record CustomerServiceAskRequest(
            Long userId,
            Long productId,
            Long skuId,
            String question,
            String clientRequestId) {
    }

    /** Request shape for /api/v1/me/ai; the user is derived from the server scope. */
    public record ScopedCustomerServiceAskRequest(
            Long productId,
            Long skuId,
            String question,
            String clientRequestId) {
    }

    /** Request shape for /api/v1/operator/ai; the actor is derived from OperatorScope. */
    public record OperatorCustomerServiceAskRequest(
            Long productId,
            Long skuId,
            String question,
            String clientRequestId) {
    }

    public record BusinessFacts(
            String question,
            long productId,
            String productCode,
            String productName,
            String productStatus,
            String productImagePath,
            long skuId,
            String skuCode,
            String color,
            String size,
            String skuStatus,
            @JsonSerialize(using = ToStringSerializer.class) BigDecimal unitPrice,
            String currency,
            int availableStock,
            List<String> knowledgeSnippets,
            Instant queriedAt) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Provider(String name, ProviderMode mode, String model) {
    }

    public record Evidence(
            String evidenceType,
            String field,
            String displayName,
            String value,
            String sourceType,
            long sourceId) {
    }

    public record TraceStep(String step, String status, String displayName, long durationMs, String detail) {
    }

    public record CustomerServiceAnswer(
            String traceId,
            String clientRequestId,
            String answer,
            AnswerStatus answerStatus,
            Provider provider,
            List<Evidence> evidence,
            BusinessFacts businessFacts,
            List<TraceStep> trace,
            long latencyMs,
            boolean fallbackUsed,
            String warning,
            Instant createdAt) {
    }

    public record PythonCustomerServiceRequest(
            String traceId,
            String clientRequestId,
            String question,
            BusinessFacts businessFacts) {
    }

    public record PythonCustomerServiceResponse(
            String traceId,
            String answer,
            AnswerStatus answerStatus,
            Provider provider,
            String warning) {
    }
}
