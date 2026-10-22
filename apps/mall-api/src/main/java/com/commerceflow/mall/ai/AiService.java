package com.commerceflow.mall.ai;

import com.commerceflow.mall.catalog.CatalogRepository;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.ai.application.port.out.CustomerServiceProvider;
import com.commerceflow.mall.ai.ratelimit.AiRateLimiter;
import com.commerceflow.mall.ai.ratelimit.RateLimitDecision;
import com.commerceflow.mall.ai.ratelimit.RateLimitExceededException;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_ANSWER_LENGTH = 600;
    private static final int MAX_DECODED_POLICY_BYTES = 2048;
    private static final Pattern CLIENT_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final Pattern FRAMED_BASE64 = Pattern.compile(
            "(?i)\\b(BASE64URL|B64URL|BASE64|B64):([A-Za-z0-9_+/-]{8,4096}={0,2})");
    private static final Pattern FRAMED_HEX = Pattern.compile("(?i)\\bHEX:([0-9A-F]{8,4096})\\b");
    private static final Pattern FRAMED_ROT13 = Pattern.compile("(?i)\\bROT13:([A-Za-z][A-Za-z0-9 ,.!?'’-]{7,256})");
    private static final Pattern NUMERIC_HTML_ENTITY = Pattern.compile("&#(?:([0-9]{1,7})|[xX]([0-9A-Fa-f]{1,6}));?");
    private static final Pattern PRICE_UPDATE_MUTATION = Pattern.compile(
            "(?:\\bupdate\\b.{0,160}\\b(?:price|sale\\s+price|unit\\s+price)\\b"
                    + "|\\b(?:change|modify|adjust|set|lower|raise)\\b.{0,24}\\b(?:price|sale\\s+price|unit\\s+price)\\b"
                    + "|\\b(?:replace|overwrite|reset)\\b.{0,48}\\b(?:sale\\s+price|unit\\s+price|price)\\b.{0,24}\\b(?:with|to|at)\\s*(?:\\$|¥)?(?:\\d+(?:\\.\\d+)?|one\\s+dollar|zero)\\b"
                    + "|\\bmake\\b.{0,48}\\b(?:product\\s+)?(?:cost|price)\\b.{0,24}(?:\\$|¥)?(?:\\d+(?:\\.\\d+)?|one\\s+dollar|zero)\\b"
                    + "|(?:把|将|请|帮我).{0,16}(?:价格|售价|标价|单价).{0,12}(?:改|修改|更新|调整|替换|重设|覆盖|调到|调为|设成|设为|改成|改为|变成|变为|降低|提高|下调|上调)"
                    + "|(?:改|修改|更新|调整|替换|重设|覆盖|调到|调为|设成|设为|改成|改为|变成|变为|降低|提高|下调|上调).{0,12}(?:价格|售价|标价|单价).{0,16}(?:为|成|到)\\s*(?:¥|￥|\\$)?\\d)");
    private static final Pattern INVENTORY_UPDATE_MUTATION = Pattern.compile(
            "(?:\\bupdate\\b.{0,160}\\b(?:inventory|stock)\\b"
                    + "|\\b(?:replace|overwrite|reset)\\b.{0,48}\\b(?:inventory|stock)\\b.{0,24}\\b(?:to|at|as|with)\\s*(?:\\d+(?:\\.\\d+)?|zero|none)\\b"
                    + "|\\b(?:inventory|stock)\\b.{0,32}\\b(?:should|must)\\b.{0,20}\\b(?:be|become|equal)\\b.{0,12}(?:\\d+(?:\\.\\d+)?|zero|none)\\b"
                    + "|\\b(?:set|reset|record|adjust|change|replace|overwrite)\\b.{0,48}\\b(?:inventory|stock)\\b.{0,24}\\b(?:to|at|as|with)\\s*(?:\\d+(?:\\.\\d+)?|zero|none)\\b"
                    + "|(?:库存|现货|存量).{0,20}(?:应该|必须|应当|需要|需).{0,16}(?:调整为|修改为|更新为|改为|设置为|设为|变为|是|为)\\s*(?:\\d|零|一|二|三|四|五|六|七|八|九)"
                    + "|(?:把|将|请|帮我).{0,16}(?:库存|现货|存量).{0,20}(?:改|修改|更新|调整|提高|增加|减少|降低|清零|设成|设为|改成|变成|调高|调低)"
                    + "|(?:改|修改|更新|调整|提高|增加|减少|降低|清零|设成|设为|改成|变成|调高|调低).{0,12}(?:库存|现货|存量).{0,20}(?:为|成|到)\\s*(?:\\d|零|一|二|三|四|五|六|七|八|九))");

    private final CatalogRepository catalog;
    private final CustomerServiceProvider provider;
    private final AiTraceRepository traces;
    private final AiRateLimiter rateLimiter;
    private final AiFallbackProperties fallbackProperties;
    private final AiProviderMetrics providerMetrics;
    private final AiProviderProperties providerProperties;

    public AiService(CatalogRepository catalog, CustomerServiceProviderClient providerClient, AiTraceRepository traces,
                     AiRateLimiter rateLimiter, AiFallbackProperties fallbackProperties,
                     AiProviderMetrics providerMetrics, AiProviderProperties providerProperties) {
        this.catalog = catalog;
        this.provider = providerClient;
        this.traces = traces;
        this.rateLimiter = rateLimiter;
        this.fallbackProperties = fallbackProperties;
        this.providerMetrics = providerMetrics;
        this.providerProperties = providerProperties;
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
            AiModels.PythonCustomerServiceResponse response = provider.answer(pythonRequest);
            boolean providerReportedError = response != null
                    && (response.answerStatus() == AiModels.AnswerStatus.PROVIDER_ERROR
                    || response.answerStatus() == AiModels.AnswerStatus.FALLBACK_ANSWER);
            stageStarted = System.nanoTime();
            validatePythonResponse(response, traceId);
            providerMetrics.record(providerReportedError ? "PROVIDER_ERROR" : "SUCCESS", elapsedMs(providerStarted));
            steps.add(step(
                    "PROVIDER_COMPLETED",
                    providerReportedError ? "FALLBACK" : "COMPLETED",
                    providerReportedError ? "AI 服务已降级" : "AI 服务已完成",
                    elapsedMs(providerStarted),
                    providerReportedError ? "提供方返回错误状态" : "已收到结构化回答"));

            if (providerReportedError) {
                outcome = fallbackOrProviderError(request.question(), facts, "PROVIDER_INVALID_RESPONSE", "AI 服务未返回可用回答，已由 Java 基于当前商品事实生成说明。");
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
            providerMetrics.record(metricOutcome(ex.kind()), elapsedMs(providerStarted));
            String errorCode = ex.safeErrorCode();
            steps.add(step("PROVIDER_COMPLETED", "FALLBACK", "AI 服务已降级", elapsedMs(providerStarted), errorCode));
            stageStarted = System.nanoTime();
            outcome = fallbackOrProviderError(request.question(), facts, errorCode, "AI 服务暂时不可用，以下回答由 Java 基于当前商品事实生成。");
            steps.add(step("RESPONSE_VALIDATED", outcome.fallbackUsed() ? "FALLBACK" : "ERROR",
                    outcome.fallbackUsed() ? "已生成降级结果" : "未生成降级回答", elapsedMs(stageStarted),
                    "提供方响应未通过本地可用性校验"));
        } catch (InvalidProviderResponseException ex) {
            providerMetrics.record("INVALID_RESPONSE", elapsedMs(providerStarted));
            stageStarted = System.nanoTime();
            outcome = fallbackOrProviderError(request.question(), facts, "PROVIDER_INVALID_RESPONSE", "AI 服务返回格式无效，以下回答由 Java 基于当前商品事实生成。");
            steps.add(step("RESPONSE_VALIDATED", outcome.fallbackUsed() ? "FALLBACK" : "ERROR",
                    outcome.fallbackUsed() ? "已生成降级结果" : "未生成降级回答", elapsedMs(stageStarted), "AI_INVALID_RESPONSE"));
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

    private ProviderOutcome fallbackOrThrow(String question, AiModels.BusinessFacts facts, String errorCode, String warning) {
        return fallbackOrProviderError(question, facts, errorCode, warning);
    }
    private ProviderOutcome fallback(String question, AiModels.BusinessFacts facts, String errorCode, String warning) {
        List<FallbackQuestionType> questionTypes = classifyFallbackQuestionIntents(question);
        if (questionTypes.contains(FallbackQuestionType.UNSUPPORTED)) {
            return fallbackOutcome(
                    "当前本地商品事实客服只支持价格、颜色尺码、库存、是否可购买和 SKU 编码问题；暂不支持该问题。",
                    AiModels.AnswerStatus.UNSUPPORTED_QUESTION,
                    errorCode,
                    warning + " 该问题不属于本地商品事实客服支持范围。");
        }

        boolean priceRequested = questionTypes.contains(FallbackQuestionType.PRICE);
        List<String> answers = questionTypes.stream().map(questionType -> switch (questionType) {
            case PRICE -> "%s %s %s 的售价为 ¥%s。".formatted(
                    facts.productName(), facts.color(), facts.size(), price(facts.unitPrice()));
            case SPECIFICATION -> "当前选择的 SKU 为 %s %s，SKU 编码为 %s。".formatted(
                    facts.color(), facts.size(), facts.skuCode());
            case SKU_CODE -> "该 SKU 编码为 %s。".formatted(facts.skuCode());
            case STOCK_OR_PURCHASE -> stockOrPurchaseAnswer(facts, !priceRequested);
            case UNSUPPORTED -> throw new IllegalStateException("Unsupported question was handled above");
        }).toList();
        return fallbackOutcome(String.join(" ", answers), AiModels.AnswerStatus.FALLBACK_ANSWER, errorCode, warning);
    }

    private ProviderOutcome fallbackOrProviderError(String question, AiModels.BusinessFacts facts, String errorCode, String warning) {
        if (!fallbackProperties.enabled()) {
            throw new CommerceException("AI_SERVICE_UNAVAILABLE", "AI provider is unavailable.");
        }
        if (providerProperties.isFallbackEnabled()) {
            return fallback(question, facts, errorCode, warning);
        }
        String model = providerProperties.getModel().isBlank() ? null : providerProperties.getModel();
        AiModels.ProviderMode mode = providerProperties.isOpenAiCompatible()
                ? AiModels.ProviderMode.REAL_OPENAI_COMPATIBLE
                : AiModels.ProviderMode.MOCK;
        return new ProviderOutcome(
                "当前 AI 提供方暂时不可用，未生成可交付的客服回答，请稍后重试。",
                AiModels.AnswerStatus.PROVIDER_ERROR,
                new AiModels.Provider(providerProperties.getProviderName(), mode, model),
                false,
                warning + " 未启用本地降级。",
                 errorCode);
    }

    private String metricOutcome(AiProviderClientException.Kind kind) {
        return switch (kind) {
            case TIMEOUT -> "TIMEOUT";
            case UNAVAILABLE -> "UNAVAILABLE";
            case INVALID_RESPONSE, FACT_MISMATCH -> "INVALID_RESPONSE";
            case PROVIDER_ERROR, CONFIGURATION, PROTOCOL_UNSUPPORTED, HTTP_ERROR -> "PROVIDER_ERROR";
        };
    }

    private ProviderOutcome fallbackOutcome(String answer, AiModels.AnswerStatus status, String errorCode, String warning) {
        return new ProviderOutcome(answer, status,
                new AiModels.Provider("java-fact-fallback", AiModels.ProviderMode.FALLBACK, null), true, warning, errorCode);
    }

    private String stockOrPurchaseAnswer(AiModels.BusinessFacts facts) {
        return stockOrPurchaseAnswer(facts, true);
    }

    private String stockOrPurchaseAnswer(AiModels.BusinessFacts facts, boolean includePrice) {
        boolean sellable = "ON_SALE".equals(facts.productStatus()) && "ON_SALE".equals(facts.skuStatus());
        if (!sellable) return "该商品当前不是上架销售状态，暂时无法购买。";
        if (facts.availableStock() == 0) return "该 SKU 当前库存为 0，暂时无法购买。";
        String answer = "%s %s 码当前库存为 %d 件，可以购买。".formatted(
                facts.color(), facts.size(), facts.availableStock());
        if (includePrice) answer = answer.substring(0, answer.length() - 1) + "，售价为 ¥%s。".formatted(price(facts.unitPrice()));
        return answer;
    }

    private boolean questionRequestsPriceAndStock(String question) {
        String normalized = normalizePolicyText(question);
        String[] clauses = normalized.split(
                "[。！？!?；;，,]+|\\b(?:and|also|but|as well as)\\b|以及|同时|另外|还有|顺便|和|并且");
        boolean asksPrice = false;
        boolean asksStock = false;
        for (String clause : clauses) {
            boolean stockClause = mentions(clause,
                    "库存", "有货", "现货", "余量", "可售", "购买", "能买", "可买", "下单",
                    "stock", "inventory", "available", "remain", "remaining", "how many", "count", "units", "items", "left");
            asksStock |= stockClause;
            asksPrice |= mentions(clause,
                    "价格", "售价", "多少钱", "价钱", "标价", "金额", "销售金额", "单价", "price", "cost")
                    || (clause.contains("how much") && !stockClause);
        }
        return asksPrice && asksStock;
    }

    private FallbackQuestionType classifyFallbackQuestion(String question) {
        return classifyFallbackQuestionIntents(question).get(0);
    }

    private List<FallbackQuestionType> classifyFallbackQuestionIntents(String question) {
        String normalized = normalizePolicyText(question);
        if (PRICE_UPDATE_MUTATION.matcher(normalized).find() || INVENTORY_UPDATE_MUTATION.matcher(normalized).find()
                || mentions(normalized, "发货", "物流", "快递", "退款", "退货", "支付", "付款", "优惠", "折扣", "订单", "其他用户",
                "改价", "修改价格", "改库存", "修改库存", "system prompt", "developer message", "api key", "authorization header",
                "access token", "secret key", "ignore all rules", "ignore previous", "系统prompt", "系统提示",
                "提示词", "忽略规则", "忽略之前", "script", "javascript", "modify price", "change price", "adjust price",
                "set price", "lower price", "raise price", "modify inventory", "change inventory", "adjust inventory",
                "set inventory", "increase inventory", "decrease inventory")) {
            return List.of(FallbackQuestionType.UNSUPPORTED);
        }
        List<FallbackIntentMatch> matches = new ArrayList<>();
        addFallbackIntentMatch(matches, normalized, FallbackQuestionType.SKU_CODE, 0,
                "sku code", "sku编码", "sku 编码", "编码", "货号", "货品代码", "商品编号", "货品编号", "产品代码", "商品代码", "产品编号");
        addFallbackIntentMatch(matches, normalized, FallbackQuestionType.SPECIFICATION, 1,
                "颜色", "尺码", "尺寸", "规格", "color", "size", "specification");
        addFallbackIntentMatch(matches, normalized, FallbackQuestionType.PRICE, 2,
                "价格", "售价", "多少钱", "多少元", "价钱", "价位", "标价", "金额", "销售金额", "单价", "price", "cost", "how much");
        addFallbackIntentMatch(matches, normalized, FallbackQuestionType.STOCK_OR_PURCHASE, 3,
                "库存", "有货", "现货", "余量", "可售", "购买", "能买", "可买", "下单", "剩余", "还剩",
                "库存数量", "库存总量", "库存数", "库存多少", "库存有多少", "库存还有多少", "还有多少件", "剩余多少",
                "stock", "in stock", "available", "purchase", "buy", "how many", "count", "remaining", "units", "items", "left");
        if (matches.isEmpty()) return List.of(FallbackQuestionType.UNSUPPORTED);
        return matches.stream()
                .sorted(Comparator.comparingInt(FallbackIntentMatch::position).thenComparingInt(FallbackIntentMatch::priority))
                .map(FallbackIntentMatch::type)
                .toList();
    }

    private void addFallbackIntentMatch(
            List<FallbackIntentMatch> matches, String question, FallbackQuestionType type, int priority, String... terms) {
        int position = firstMentionPosition(question, terms);
        if (position >= 0) matches.add(new FallbackIntentMatch(position, priority, type));
    }

    private int firstMentionPosition(String question, String... terms) {
        int first = Integer.MAX_VALUE;
        for (String term : terms) {
            int found = question.indexOf(term);
            if (found >= 0) first = Math.min(first, found);
        }
        return first == Integer.MAX_VALUE ? -1 : first;
    }

    private String normalizePolicyText(String question) {
        String decoded = question;
        for (int round = 0; round < 2; round++) {
            String next = decodeFramedPolicyText(decodeNumericHtmlEntities(decodePercentEncoding(decoded)));
            if (next.equals(decoded)) break;
            decoded = next;
        }
        String compatible = Normalizer.normalize(decoded, Normalizer.Form.NFKC);
        StringBuilder visible = new StringBuilder(compatible.length());
        compatible.codePoints()
                .filter(codePoint -> Character.getType(codePoint) != Character.FORMAT)
                .forEach(visible::appendCodePoint);
        String lowered = visible.toString().toLowerCase(Locale.ROOT);
        StringBuilder policyText = new StringBuilder(lowered.length());
        lowered.codePoints()
                .map(this::normalizeCommonScriptConfusable)
                .forEach(policyText::appendCodePoint);
        return policyText.toString().trim().replaceAll("\\s+", " ");
    }

    private String decodePercentEncoding(String value) {
        StringBuilder decoded = new StringBuilder(value.length());
        for (int index = 0; index < value.length();) {
            if (value.charAt(index) == '%' && index + 2 < value.length()) {
                int high = asciiHexValue(value.charAt(index + 1));
                int low = asciiHexValue(value.charAt(index + 2));
                if (high >= 0 && low >= 0) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    while (index + 2 < value.length() && value.charAt(index) == '%') {
                        high = asciiHexValue(value.charAt(index + 1));
                        low = asciiHexValue(value.charAt(index + 2));
                        if (high < 0 || low < 0) break;
                        bytes.write((high << 4) | low);
                        index += 3;
                    }
                    decoded.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                    continue;
                }
            }
            decoded.append(value.charAt(index++));
        }
        return decoded.toString();
    }

    private int asciiHexValue(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        return -1;
    }

    private String decodeNumericHtmlEntities(String value) {
        return replaceMatches(value, NUMERIC_HTML_ENTITY, match -> {
            try {
                String decimal = match.group(1);
                int codePoint = decimal == null
                        ? Integer.parseInt(match.group(2), 16)
                        : Integer.parseInt(decimal);
                if (!Character.isValidCodePoint(codePoint)
                        || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)) {
                    return match.group(0);
                }
                return new String(Character.toChars(codePoint));
            } catch (NumberFormatException ex) {
                return match.group(0);
            }
        });
    }

    private String decodeFramedPolicyText(String value) {
        String base64Decoded = replaceMatches(value, FRAMED_BASE64, match -> {
            String token = match.group(2).replace('-', '+').replace('_', '/');
            try {
                byte[] bytes = Base64.getDecoder().decode(token);
                return decodePrintableUtf8(bytes, match.group(0));
            } catch (IllegalArgumentException ex) {
                return match.group(0);
            }
        });
        String hexDecoded = replaceMatches(base64Decoded, FRAMED_HEX, match -> {
            try {
                return decodePrintableUtf8(HexFormat.of().parseHex(match.group(1)), match.group(0));
            } catch (IllegalArgumentException ex) {
                return match.group(0);
            }
        });
        return replaceMatches(hexDecoded, FRAMED_ROT13, match -> decodeRot13(match.group(1)));
    }

    private String decodePrintableUtf8(byte[] bytes, String fallback) {
        if (bytes.length == 0 || bytes.length > MAX_DECODED_POLICY_BYTES) return fallback;
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            if (decoded.isEmpty() || !isPrintablePolicyText(decoded)) return fallback;
            return decoded;
        } catch (CharacterCodingException ex) {
            return fallback;
        }
    }

    private boolean isPrintablePolicyText(String value) {
        return value.codePoints().allMatch(codePoint -> {
            int type = Character.getType(codePoint);
            return (type != Character.CONTROL || codePoint == '\n' || codePoint == '\r' || codePoint == '\t')
                    && type != Character.FORMAT
                    && type != Character.SURROGATE
                    && type != Character.PRIVATE_USE
                    && type != Character.UNASSIGNED
                    && type != Character.LINE_SEPARATOR
                    && type != Character.PARAGRAPH_SEPARATOR;
        });
    }

    private String decodeRot13(String value) {
        StringBuilder decoded = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= 'A' && character <= 'Z') {
                decoded.append((char) ('A' + (character - 'A' + 13) % 26));
            } else if (character >= 'a' && character <= 'z') {
                decoded.append((char) ('a' + (character - 'a' + 13) % 26));
            } else {
                decoded.append(character);
            }
        }
        return decoded.toString();
    }

    private String replaceMatches(String value, Pattern pattern, Function<Matcher, String> replacement) {
        Matcher matcher = pattern.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.apply(matcher)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private int normalizeCommonScriptConfusable(int codePoint) {
        return switch (codePoint) {
            case 0x0131 -> 'i'; // ı, Latin small letter dotless i
            case 0x0430 -> 'a'; // а
            case 0x0432 -> 'b'; // в
            case 0x0441 -> 'c'; // с
            case 0x0435 -> 'e'; // е
            case 0x043D -> 'h'; // н
            case 0x0456 -> 'i'; // і
            case 0x0458 -> 'j'; // ј
            case 0x043A -> 'k'; // к
            case 0x043C -> 'm'; // м
            case 0x043E -> 'o'; // о
            case 0x0440 -> 'p'; // р
            case 0x0455 -> 's'; // ѕ
            case 0x0442 -> 't'; // т
            case 0x0443 -> 'y'; // у
            case 0x0445 -> 'x'; // х
            case 0x04CF -> 'l'; // ӏ
            case 0x03B1 -> 'a'; // α
            case 0x03B2 -> 'b'; // β
            case 0x03B5 -> 'e'; // ε
            case 0x03B7 -> 'n'; // η
            case 0x03B9 -> 'i'; // ι
            case 0x03BA -> 'k'; // κ
            case 0x03BC -> 'm'; // μ
            case 0x03BD -> 'v'; // ν
            case 0x03BF -> 'o'; // ο
            case 0x03C1 -> 'p'; // ρ
            case 0x03C2, 0x03C3 -> 's'; // ς, σ
            case 0x03C4 -> 't'; // τ
            case 0x03C5 -> 'y'; // υ
            case 0x03C7 -> 'x'; // χ
            case 0x0585 -> 'o'; // օ, Armenian small letter oh (UTS #39 18.0.0)
            case 0x0582 -> 'i'; // ւ, Armenian small letter yiwn (UTS #39 18.0.0)
            case 0x13A5 -> 'i'; // Ꭵ, Cherokee letter V (UTS #39 18.0.0)
            case 0xAB75 -> 'i'; // ꭵ, Cherokee small letter V (after Unicode lowercasing)
            default -> codePoint;
        };
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

    private record FallbackIntentMatch(int position, int priority, FallbackQuestionType type) {
    }

    private static class InvalidProviderResponseException extends RuntimeException {
    }
}
