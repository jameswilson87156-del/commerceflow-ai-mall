package com.commerceflow.mall.ai;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.commerceflow.mall.ai.ratelimit.RateLimitHeaders;
import com.commerceflow.mall.operations.LegacyApiPolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CommerceApiContract.AI_PATH)
public class AiController {
    private final AiService service;
    private final LegacyApiPolicy legacyApi;
    public AiController(AiService service, LegacyApiPolicy legacyApi) { this.service=service; this.legacyApi=legacyApi; }

    @PostMapping(CommerceApiContract.AI_CUSTOMER_SERVICE_ASK_PATH)
    public ResponseEntity<AiModels.CustomerServiceAnswer> ask(
            @Valid @RequestBody AiModels.CustomerServiceAskRequest request,
            HttpServletRequest servletRequest) {
        legacyApi.requireEnabled();
        AiService.AiRequestResult result = service.askWithRateLimit(request, servletRequest.getRemoteAddr());
        return ResponseEntity.ok().headers(RateLimitHeaders.success(result.rateLimit())).body(result.answer());
    }

    /**
     * The fixed-SKU prototype cannot represent a real user selection. Keep a clear
     * migration response instead of maintaining a second, conflicting AI flow.
     */
    @Deprecated(forRemoval = false)
    @PostMapping(CommerceApiContract.AI_LEGACY_PRODUCT_CHAT_PATH)
    public void deprecatedProductChat(@Valid @RequestBody ApiModels.ProductChatRequest request) {
        legacyApi.requireEnabled();
        throw new com.commerceflow.mall.core.CommerceException(
                "LEGACY_ENDPOINT_DEPRECATED", "Use POST /api/ai/customer-service/ask with productId and skuId");
    }
}
