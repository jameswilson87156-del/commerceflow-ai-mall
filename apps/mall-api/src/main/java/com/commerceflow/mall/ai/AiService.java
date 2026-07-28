package com.commerceflow.mall.ai;

import com.commerceflow.mall.catalog.CatalogRepository;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.ai.ratelimit.AiRateLimiter;
import com.commerceflow.mall.ai.ratelimit.RateLimitDecision;
import com.commerceflow.mall.ai.ratelimit.RateLimitExceededException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_ANSWER_LENGTH = 600;
    private static final Pattern CLIENT_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");

    private final CatalogRepository catalog;
    private final CustomerServiceProviderClient providerClient;
    private final AiTraceRepository traces;
    private final AiRateLimiter rateLimiter;

    public AiService(CatalogRepository catalog, CustomerServiceProviderClient providerClient, AiTraceRepository traces, AiRateLimiter rateLimiter) {
        this.catalog = catalog;
        this.providerClient = providerClient;
        this.traces = traces;
        this.rateLimiter = rateLimiter;
    }

    public AiModels.CustomerServiceAnswer ask(AiModels.CustomerServiceAskRequest rawRequest) {
        return askWithRateLimit(rawRequest, "127.0.0.1").answer();
    }

    public AiRequestResult askWithRateLimit(AiModels.CustomerServiceAskRequest rawRequest, String remoteAddress) {
        ValidatedRequest request = validate(rawRequest);
        RateLimitDecision decision = rateLimiter.check(request.userId(), remoteAddress);
        if (!decision.allowed()) throw new RateLimitExceededException(decision);
        return new AiRequestResult(askValidated(request), decision);
    }

    private AiModels.CustomerServiceAnswer askValidated(ValidatedRequest request) {
        long requestStarted = System.nanoTime();
        List<AiModels.TraceStep> steps = new ArrayList<>();

        long stageStarted = System.nanoTime();
        steps.add(step("REQUEST_RECEIVED", "COMPLETED", "已接收请求", elapsedMs(stageStarted), "已校验请求参数"));

        stageStarted = System.nanoTime();
        CatalogRepository.CustomerServiceSelection selection = selection(request.productId(), request.skuId());
        Instant createdAt = Instant.now();
        AiModels.BusinessFacts facts = facts(request.question(), selection, createdAt);
        steps.add(step("BUSINESS_FACTS_LOADED", "COMPLETED", "已加载商品事实", elapsedMs(stageStarted), "来自 Product、SKU 与 Inventory"));

        stageStarted = System.nanoTime();
        String traceId = UUID.randomUUID().toString();
        AiModels.PythonCustomerServiceRequest pythonRequest =
                new AiModels.PythonCustomerServiceRequest(traceId, request.clientRequestId(), request.question(), facts);
        steps.add(step("PYTHON_REQUEST_SENT", "COMPLETED", "已准备 AI 服务请求", elapsedMs(stageStarted), "已构造受限 businessFacts"));

        ProviderOutcome outcome;
        long providerStarted = System.nanoTime();
        try {
            AiModels.PythonCustomerServiceResponse response = providerClient.answer(pythonRequest);
            boolean providerReportedError = response != null
                    && (response.answerStatus() == AiModels.AnswerStatus.PROVIDER_ERROR
                    || response.answerStatus() == AiModels.AnswerStatus.FALLBACK_ANSWER);
            steps.add(step(
                    "PROVIDER_COMPLETED",
                    providerReportedError ? "FALLBACK" : "COMPLETED",
                    providerReportedError ? "AI 服务已降级" : "AI 服务已完成",
                    elapsedMs(providerStarted),
                    providerReportedError ? "提供方返回错误状态" : "已收到结构化回答"));

            stageStarted = System.nanoTime();
            validatePythonResponse(response, traceId);
            if (providerReportedError) {
                outcome = fallback(request.question(), facts, "AI_PROVIDER_ERROR", "AI 服务未返回可用回答，已由 Java 基于当前商品事实生成说明。");
            } else {
                outcome = new ProviderOutcome(response.answer(), response.answerStatus(), response.provider(), false, response.warning(), null);
            }
            steps.add(step(
                    "RESPONSE_VALIDATED",
                    outcome.fallbackUsed() ? "FALLBACK" : "COMPLETED",
                    outcome.fallbackUsed() ? "已校验并生成降级结果" : "已校验 AI 响应",
                    elapsedMs(stageStarted),
                    outcome.fallbackUsed() ? "已校验提供方状态并应用本地边界" : "Java 已校验状态、提供方与回答内容"));
        } catch (AiProviderClientException ex) {
            String errorCode = switch (ex.kind()) {
                case TIMEOUT -> "AI_SERVICE_TIMEOUT";
                case UNAVAILABLE -> "AI_SERVICE_UNAVAILABLE";
                case INVALID_RESPONSE -> "AI_INVALID_RESPONSE";
                case PROVIDER_ERROR -> "AI_PROVIDER_ERROR";
            };
            steps.add(step("PROVIDER_COMPLETED", "FALLBACK", "AI 服务已降级", elapsedMs(providerStarted), errorCode));
            stageStarted = System.nanoTime();
            outcome = fallback(request.question(), facts, errorCode, "AI 服务暂时不可用，以下回答由 Java 基于当前商品事实生成。");
            steps.add(step("RESPONSE_VALIDATED", "FALLBACK", "已生成降级结果", elapsedMs(stageStarted), "提供方响应未通过本地可用性校验"));
        } catch (InvalidProviderResponseException ex) {
            stageStarted = System.nanoTime();
            outcome = fallback(request.question(), facts, "AI_INVALID_RESPONSE", "AI 服务返回格式无效，以下回答由 Java 基于当前商品事实生成。");
            steps.add(step("RESPONSE_VALIDATED", "FALLBACK", "已生成降级结果", elapsedMs(stageStarted), "AI_INVALID_RESPONSE"));
        }

        stageStarted = System.nanoTime();
        List<AiModels.Evidence> evidence = evidence(facts);
        steps.add(step("RESPONSE_RETURNED", "COMPLETED", "已组装响应", elapsedMs(stageStarted), "已附加 Java Evidence 与 Trace，准备返回客户端"));
        long latencyMs = elapsedMs(requestStarted);
        AiModels.CustomerServiceAnswer answer = new AiModels.CustomerServiceAnswer(
                traceId, request.clientRequestId(), outcome.answer(), outcome.status(), outcome.provider(), evidence, facts,
                List.copyOf(steps), latencyMs, outcome.fallbackUsed(), outcome.warning(), createdAt);
        // Trace persistence is deliberately outside the response latency reported to the client.
        traces.save(answer, request.userId(), request.productId(), request.skuId(), questionSummary(request.question()), outcome.errorCode());
        return answer;
    }

    private ValidatedRequest validate(AiModels.CustomerServiceAskRequest request) {
        if (request == null || positive(request.userId()) == null || positive(request.productId()) == null || positive(request.skuId()) == null) {
            throw new CommerceException("INVALID_REQUEST", "userId, productId and skuId must be positive");
        }
        String question = request.question() == null ? "" : request.question().trim();
        if (question.isBlank()) throw new CommerceException("INVALID_QUESTION", "question must not be blank");
        if (question.length() > MAX_QUESTION_LENGTH) throw new CommerceException("QUESTION_TOO_LONG", "question must be 500 characters or fewer");
        String clientRequestId = request.clientRequestId() == null ? "" : request.clientRequestId().trim();
        if (!CLIENT_REQUEST_ID.matcher(clientRequestId).matches()) {
            throw new CommerceException("INVALID_CLIENT_REQUEST_ID", "clientRequestId must use 1-80 letters, digits, dot, underscore, or hyphen");
        }
        return new ValidatedRequest(request.userId(), request.productId(), request.skuId(), question, clientRequestId);
    }

    private Long positive(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private CatalogRepository.CustomerServiceSelection selection(long productId, long skuId) {
        if (!catalog.productExists(productId)) throw new CommerceException("PRODUCT_NOT_FOUND", "Product does not exist");
        if (!catalog.skuExists(skuId)) throw new CommerceException("SKU_NOT_FOUND", "SKU does not exist");
        return catalog.findCustomerServiceSelection(productId, skuId)
                .orElseThrow(() -> new CommerceException("PRODUCT_SKU_MISMATCH", "SKU does not belong to product"));
    }

    private AiModels.BusinessFacts facts(String question, CatalogRepository.CustomerServiceSelection selection, Instant queriedAt) {
        return new AiModels.BusinessFacts(question, selection.productId(), selection.productCode(), selection.productName(),
                selection.productStatus(), selection.skuImagePath(), selection.skuId(), selection.skuCode(), selection.color(),
                selection.size(), selection.skuStatus(), selection.unitPrice(), selection.currency(), selection.availableStock(), List.of(), queriedAt);
    }

    private void validatePythonResponse(AiModels.PythonCustomerServiceResponse response, String traceId) {
        if (response == null || !traceId.equals(response.traceId()) || response.answerStatus() == null || response.provider() == null
                || blank(response.provider().name()) || response.provider().mode() == null || blank(response.answer())
                || response.answer().length() > MAX_ANSWER_LENGTH) {
            throw new InvalidProviderResponseException();
        }
        if (response.provider().mode() == AiModels.ProviderMode.FALLBACK
                || (response.provider().mode() == AiModels.ProviderMode.MOCK && !"commerceflow-mock".equals(response.provider().name()))) {
            throw new InvalidProviderResponseException();
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ProviderOutcome fallback(String question, AiModels.BusinessFacts facts, String errorCode, String warning) {
        FallbackQuestionType questionType = classifyFallbackQuestion(question);
        if (questionType == FallbackQuestionType.UNSUPPORTED) {
            return fallbackOutcome(
                    "当前本地商品事实客服只支持价格、颜色尺码、库存、是否可购买和 SKU 编码问题；暂不支持该问题。",
                    AiModels.AnswerStatus.UNSUPPORTED_QUESTION,
                    errorCode,
                    warning + " 该问题不属于本地商品事实客服支持范围。");
        }

        String answer = switch (questionType) {
            case PRICE -> "%s %s %s 的售价为 ¥%s。".formatted(
                    facts.productName(), facts.color(), facts.size(), price(facts.unitPrice()));
            case SPECIFICATION -> "当前选择的 SKU 为 %s %s，SKU 编码为 %s。".formatted(
                    facts.color(), facts.size(), facts.skuCode());
            case SKU_CODE -> "该 SKU 编码为 %s。".formatted(facts.skuCode());
            case STOCK_OR_PURCHASE -> stockOrPurchaseAnswer(facts);
            case UNSUPPORTED -> throw new IllegalStateException("Unsupported question was handled above");
        };
        return fallbackOutcome(answer, AiModels.AnswerStatus.FALLBACK_ANSWER, errorCode, warning);
    }

    private ProviderOutcome fallbackOutcome(String answer, AiModels.AnswerStatus status, String errorCode, String warning) {
        return new ProviderOutcome(answer, status,
                new AiModels.Provider("java-fact-fallback", AiModels.ProviderMode.FALLBACK, null), true, warning, errorCode);
    }

    private String stockOrPurchaseAnswer(AiModels.BusinessFacts facts) {
        boolean sellable = "ON_SALE".equals(facts.productStatus()) && "ON_SALE".equals(facts.skuStatus());
        if (!sellable) return "该商品当前不是上架销售状态，暂时无法购买。";
        if (facts.availableStock() == 0) return "该 SKU 当前库存为 0，暂时无法购买。";
        return "%s %s 码当前库存为 %d 件，可以购买，售价为 ¥%s。".formatted(
                facts.color(), facts.size(), facts.availableStock(), price(facts.unitPrice()));
    }

    private FallbackQuestionType classifyFallbackQuestion(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        if (mentions(normalized, "发货", "物流", "快递", "退款", "退货", "支付", "付款", "优惠", "折扣", "订单", "其他用户",
                "改价", "修改价格", "改库存", "修改库存", "system prompt", "系统prompt", "系统提示", "提示词", "忽略规则", "忽略之前", "script", "javascript")) {
            return FallbackQuestionType.UNSUPPORTED;
        }
        if (mentions(normalized, "sku", "编码", "货号")) return FallbackQuestionType.SKU_CODE;
        if (mentions(normalized, "颜色", "尺码", "尺寸", "规格")) return FallbackQuestionType.SPECIFICATION;
        if (mentions(normalized, "价格", "售价", "多少钱", "价钱")) return FallbackQuestionType.PRICE;
        if (mentions(normalized, "库存", "有货", "购买", "能买", "可买", "下单")) return FallbackQuestionType.STOCK_OR_PURCHASE;
        return FallbackQuestionType.UNSUPPORTED;
    }

    private boolean mentions(String question, String... terms) {
        for (String term : terms) {
            if (question.contains(term)) return true;
        }
        return false;
    }

    private List<AiModels.Evidence> evidence(AiModels.BusinessFacts facts) {
        return List.of(
                evidence("PRODUCT_FACT", "productName", "商品名称", facts.productName(), "PRODUCT", facts.productId()),
                evidence("PRODUCT_FACT", "productStatus", "商品状态", facts.productStatus(), "PRODUCT", facts.productId()),
                evidence("SKU_FACT", "skuCode", "SKU 编码", facts.skuCode(), "SKU", facts.skuId()),
                evidence("SKU_FACT", "color", "颜色", facts.color(), "SKU", facts.skuId()),
                evidence("SKU_FACT", "size", "尺寸", facts.size(), "SKU", facts.skuId()),
                evidence("PRICE_FACT", "unitPrice", "售价", price(facts.unitPrice()) + " " + facts.currency(), "SKU", facts.skuId()),
                evidence("INVENTORY_FACT", "availableStock", "当前库存", String.valueOf(facts.availableStock()), "INVENTORY", facts.skuId()));
    }

    private AiModels.Evidence evidence(String type, String field, String displayName, String value, String sourceType, long sourceId) {
        return new AiModels.Evidence(type, field, displayName, value, sourceType, sourceId);
    }

    private String price(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }

    private String questionSummary(String question) {
        return switch (classifyFallbackQuestion(question)) {
            case PRICE -> "商品客服：价格咨询";
            case SPECIFICATION -> "商品客服：规格咨询";
            case SKU_CODE -> "商品客服：SKU 编码咨询";
            case STOCK_OR_PURCHASE -> "商品客服：库存或购买咨询";
            case UNSUPPORTED -> "商品客服：暂不支持的问题";
        };
    }

    private AiModels.TraceStep step(String step, String status, String displayName, long durationMs, String detail) {
        return new AiModels.TraceStep(step, status, displayName, durationMs, detail);
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private record ValidatedRequest(long userId, long productId, long skuId, String question, String clientRequestId) {
    }

    public record AiRequestResult(AiModels.CustomerServiceAnswer answer, RateLimitDecision rateLimit) {
    }

    private record ProviderOutcome(
            String answer,
            AiModels.AnswerStatus status,
            AiModels.Provider provider,
            boolean fallbackUsed,
            String warning,
            String errorCode) {
    }

    private enum FallbackQuestionType {
        PRICE,
        SPECIFICATION,
        SKU_CODE,
        STOCK_OR_PURCHASE,
        UNSUPPORTED
    }

    private static class InvalidProviderResponseException extends RuntimeException {
    }
}
