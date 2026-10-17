package com.commerceflow.mall.operations;

import com.commerceflow.mall.account.application.policy.OperatorAuthorizationPolicy;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.order.timeout.OrderTimeoutCancellationService;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator-only trigger so the timeout rule can be demonstrated and verified
 * without waiting for the fixed-delay scan. The scan itself keeps running.
 */
@RestController
@RequestMapping(CommerceApiContract.V1_OPERATOR_OPERATIONS_PATH)
public class OperatorOrderTimeoutController {
    private final OrderTimeoutCancellationService service;
    private final OperatorAuthorizationPolicy operatorPolicy;

    public OperatorOrderTimeoutController(OrderTimeoutCancellationService service, OperatorAuthorizationPolicy operatorPolicy) {
        this.service = service;
        this.operatorPolicy = operatorPolicy;
    }

    @PostMapping(CommerceApiContract.OPERATIONS_ORDER_TIMEOUT_ENFORCE_PATH)
    public Map<String, Object> enforce() {
        operatorPolicy.requireOperator();
        int cancelled = service.enforceDeadline(Instant.now(), 100);
        return Map.of("cancelled", cancelled);
    }
}
