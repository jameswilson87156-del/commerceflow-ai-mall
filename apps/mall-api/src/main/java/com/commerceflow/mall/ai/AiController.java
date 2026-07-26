package com.commerceflow.mall.ai;

import com.commerceflow.mall.api.ApiModels;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService service;
    public AiController(AiService service) { this.service=service; }

    @PostMapping("/customer-service/ask")
    public AiModels.CustomerServiceAnswer ask(@Valid @RequestBody AiModels.CustomerServiceAskRequest request) {
        return service.ask(request);
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
