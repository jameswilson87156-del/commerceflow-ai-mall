package com.commerceflow.mall.ai;

import com.commerceflow.mall.account.application.policy.OperatorAuthorizationPolicy;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.ai.ratelimit.RateLimitHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operator AI API. The management actor, not the request body, supplies the identity scope. */
@RestController
@RequestMapping(CommerceApiContract.V1_OPERATOR_AI_PATH)
public class OperatorAiController {
    private final AiService service;
    private final OperatorAuthorizationPolicy operatorPolicy;

    public OperatorAiController(AiService service, OperatorAuthorizationPolicy operatorPolicy) {
        this.service = service;
        this.operatorPolicy = operatorPolicy;
    }

    @PostMapping(CommerceApiContract.V1_OPERATOR_AI_CUSTOMER_SERVICE_ASK_PATH)
    public ResponseEntity<AiModels.CustomerServiceAnswer> ask(
            @Valid @RequestBody AiModels.OperatorCustomerServiceAskRequest request,
            HttpServletRequest servletRequest) {
        long operatorId = operatorPolicy.requireOperator().operatorId();
        var scopedRequest = new AiModels.CustomerServiceAskRequest(
                operatorId, request.productId(), request.skuId(), request.question(), request.clientRequestId());
        AiService.AiRequestResult result = service.askWithRateLimit(scopedRequest, servletRequest.getRemoteAddr());
        return ResponseEntity.ok().headers(RateLimitHeaders.success(result.rateLimit())).body(result.answer());
    }
}
