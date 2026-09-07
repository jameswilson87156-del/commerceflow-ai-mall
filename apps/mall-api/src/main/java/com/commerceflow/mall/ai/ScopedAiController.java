package com.commerceflow.mall.ai;

import com.commerceflow.mall.account.application.port.out.CurrentUserPort;
import com.commerceflow.mall.ai.ratelimit.RateLimitHeaders;
import com.commerceflow.mall.api.CommerceApiContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned AI endpoint that never accepts a client-controlled user id. */
@RestController
@RequestMapping(CommerceApiContract.V1_ME_AI_PATH)
public class ScopedAiController {
    private final AiService service;
    private final CurrentUserPort currentUser;

    public ScopedAiController(AiService service, CurrentUserPort currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping(CommerceApiContract.V1_ME_AI_CUSTOMER_SERVICE_ASK_PATH)
    public ResponseEntity<AiModels.CustomerServiceAnswer> ask(
            @Valid @RequestBody AiModels.ScopedCustomerServiceAskRequest request,
            HttpServletRequest servletRequest) {
        long userId = currentUser.currentUser().userId();
        var scopedRequest = new AiModels.CustomerServiceAskRequest(
                userId, request.productId(), request.skuId(), request.question(), request.clientRequestId());
        AiService.AiRequestResult result = service.askWithRateLimit(scopedRequest, servletRequest.getRemoteAddr());
        return ResponseEntity.ok().headers(RateLimitHeaders.success(result.rateLimit())).body(result.answer());
    }
}
