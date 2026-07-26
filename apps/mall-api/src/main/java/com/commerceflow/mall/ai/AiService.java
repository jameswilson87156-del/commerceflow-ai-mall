package com.commerceflow.mall.ai;

import com.commerceflow.mall.catalog.CatalogRepository;
import com.commerceflow.mall.core.CommerceException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    public AiService(CatalogRepository catalog, CustomerServiceProviderClient providerClient, AiTraceRepository traces) {
        this.catalog = catalog;
        this.providerClient = providerClient;
        this.traces = traces;
    }

    public AiModels.CustomerServiceAnswer ask(AiModels.CustomerServiceAskRequest rawRequest) {
        ValidatedRequest request = validate(rawRequest);
        CatalogRepository.CustomerServiceSelection selection = selection(request.productId(), request.skuId());
        String traceId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        AiModels.BusinessFacts facts = facts(request.question(), selection, createdAt);
        long started = System.nanoTime();
        List<AiModels.TraceStep> steps = new ArrayList<>();
        steps.add(step("REQUEST_RECEIVED", "COMPLETED", "已接收请求", 0, "已验证请求参数"));
        steps.add(step("BUSINESS_FACTS_LOADED", "COMPLETED", "已加载商品事实", 0, "来自 Product、SKU 与 Inventory"));

        ProviderOutcome outcome;
        try {
            steps.add(step("PYTHON_REQUEST_SENT", "COMPLETED", "已调用 AI 服务", 0, "发送受限 businessFacts"));
            AiModels.PythonCustomerServiceResponse response = providerClient.answer(
                    new AiModels.PythonCustomerServiceRequest(traceId, request.clientRequestId(), request.question(), facts));
            validatePythonResponse(response, traceId);
            if (response.answerStatus() == AiModels.AnswerStatus.PROVIDER_ERROR
                    || response.answerStatus() == AiModels.AnswerStatus.FALLBACK_ANSWER) {
                outcome = fallback(facts, "AI_PROVIDER_ERROR", "AI 服务未返回可用回答，已由 Java 基于当前商品事实生成说明。");
                steps.add(step("PROVIDER_COMPLETED", "FALLBACK", "AI 服务已降级", 0, "提供方返回错误状态"));
            } else {
                outcome = new ProviderOutcome(response.answer(), response.answerStatus(), response.provider(), false, response.warning(), null);
                steps.add(step("PROVIDER_COMPLETED", "COMPLETED", "AI 服务已完成", 0, "已收到结构化回答"));
            }
        } catch (AiProviderClientException ex) {
            String errorCode = switch (ex.kind()) {
                case TIMEOUT -> "AI_SERVICE_TIMEOUT";
                case UNAVAILABLE -> "AI_SERVICE_UNAVAILABLE";
                case INVALID_RESPONSE -> "AI_INVALID_RESPONSE";
                case PROVIDER_ERROR -> "AI_PROVIDER_ERROR";
            };
            outcome = fallback(facts, errorCode, "AI 服务暂时不可用，以下回答由 Java 基于当前商品事实生成。");
            steps.add(step("PROVIDER_COMPLETED", "FALLBACK", "AI 服务已降级", 0, errorCode));
        } catch (InvalidProviderResponseException ex) {
            outcome = fallback(facts, "AI_INVALID_RESPONSE", "AI 服务返回格式无效，以下回答由 Java 基于当前商品事实生成。");
            steps.add(step("PROVIDER_COMPLETED", "FALLBACK", "AI 服务已降级", 0, "AI_INVALID_RESPONSE"));
        }

        long latencyMs = Math.max(0, (System.nanoTime() - started) / 1_000_000);
        steps.add(step("RESPONSE_VALIDATED", "COMPLETED", "已校验 AI 响应", 0, "Java 已校验状态、提供方与回答内容"));
        List<AiModels.Evidence> evidence = evidence(facts);
        steps.add(step("RESPONSE_RETURNED", "COMPLETED", "已返回结果", 0, "已附加 Java Evidence 与 Trace"));
        AiModels.CustomerServiceAnswer answer = new AiModels.CustomerServiceAnswer(
                traceId, request.clientRequestId(), outcome.answer(), outcome.status(), outcome.provider(), evidence, facts,
                List.copyOf(steps), latencyMs, outcome.fallbackUsed(), outcome.warning(), createdAt);
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

    private ProviderOutcome fallback(AiModels.BusinessFacts facts, String errorCode, String warning) {
        String answer;
        boolean sellable = "ON_SALE".equals(facts.productStatus()) && "ON_SALE".equals(facts.skuStatus());
        if (!sellable) {
            answer = "该商品当前不是上架销售状态，暂时无法购买。";
        } else if (facts.availableStock() == 0) {
            answer = "该 SKU 当前库存为 0，暂时无法购买。";
        } else {
            answer = "%s %s 码当前库存为 %d 件，可以购买，售价为 ¥%s。".formatted(
                    facts.color(), facts.size(), facts.availableStock(), price(facts.unitPrice()));
        }
        return new ProviderOutcome(answer, AiModels.AnswerStatus.FALLBACK_ANSWER,
                new AiModels.Provider("java-fact-fallback", AiModels.ProviderMode.FALLBACK, null), true, warning, errorCode);
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
        String lower = question.toLowerCase();
        if (lower.contains("库存") || lower.contains("有货") || lower.contains("购买")) return "商品客服：库存或购买咨询";
        if (lower.contains("价格") || lower.contains("售价") || lower.contains("多少钱")) return "商品客服：价格咨询";
        if (lower.contains("颜色") || lower.contains("尺码") || lower.contains("尺寸")) return "商品客服：规格咨询";
        return "商品客服：受限商品咨询";
    }

    private AiModels.TraceStep step(String step, String status, String displayName, long durationMs, String detail) {
        return new AiModels.TraceStep(step, status, displayName, durationMs, detail);
    }

    private record ValidatedRequest(long userId, long productId, long skuId, String question, String clientRequestId) {
    }

    private record ProviderOutcome(
            String answer,
            AiModels.AnswerStatus status,
            AiModels.Provider provider,
            boolean fallbackUsed,
            String warning,
            String errorCode) {
    }

    private static class InvalidProviderResponseException extends RuntimeException {
    }
}
