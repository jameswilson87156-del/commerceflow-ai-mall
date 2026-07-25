package com.commerceflow.mall.ai;

import com.commerceflow.mall.api.ApiModels;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService service;
    public AiController(AiService service) { this.service=service; }
    @PostMapping("/product-chat") public ApiModels.AiAnswer chat(@Valid @RequestBody ApiModels.ProductChatRequest request) { return service.chat(request); }
}
