package com.commerceflow.mall.ai;

import com.commerceflow.mall.api.ApiModels;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import com.commerceflow.mall.ai.ratelimit.RateLimitHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService service;
    public AiController(AiService service) { this.service=service; }

    @PostMapping("/customer-service/ask")
    public ResponseEntity<AiModels.CustomerServiceAnswer> ask(
            @Valid @RequestBody AiModels.CustomerServiceAskRequest request,
            HttpServletRequest servletRequest) {
        AiService.AiRequestResult result = service.askWithRateLimit(request, servletRequest.getRemoteAddr());
        return ResponseEntity.ok().headers(RateLimitHeaders.success(result.rateLimit())).body(result.answer());
    }

    /**
     * The fixed-SKU prototype cannot represent a real user selection. Keep a clear
     * migration response instead of maintaining a second, conflicting AI flow.
     */
    @Deprecated(forRemoval = false)
    @PostMapping("/product-chat")
    public void deprecatedProductChat(@Valid @RequestBody ApiModels.ProductChatRequest request) {
        throw new com.commerceflow.mall.core.CommerceException(
                "LEGACY_ENDPOINT_DEPRECATED", "Use POST /api/ai/customer-service/ask with productId and skuId");
    }
}
