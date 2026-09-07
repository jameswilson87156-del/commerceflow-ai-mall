package com.commerceflow.mall.order;

import com.commerceflow.mall.account.application.policy.OperatorAuthorizationPolicy;
import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.order.application.OrderReadService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cross-user order read API. Authorization is server-side and not query-parameter based. */
@RestController
@RequestMapping(CommerceApiContract.V1_OPERATOR_ORDERS_PATH)
public class OperatorOrderController {
    private final OrderReadService readService;
    private final OperatorAuthorizationPolicy operatorPolicy;

    public OperatorOrderController(OrderReadService readService, OperatorAuthorizationPolicy operatorPolicy) {
        this.readService = readService;
        this.operatorPolicy = operatorPolicy;
    }

    @GetMapping
    public List<ApiModels.OrderSummary> list() {
        operatorPolicy.requireOperator();
        return readService.listForOperator();
    }

    @GetMapping("/{orderNo}/execution-evidence")
    public OrderExecutionEvidenceDto evidence(@PathVariable String orderNo) {
        operatorPolicy.requireOperator();
        return readService.evidenceForOperator(orderNo);
    }

    @GetMapping("/{orderNo}")
    public ApiModels.OrderSummary detail(@PathVariable String orderNo) {
        operatorPolicy.requireOperator();
        return readService.detailForOperator(orderNo);
    }
}
