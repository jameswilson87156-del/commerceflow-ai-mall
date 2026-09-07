package com.commerceflow.mall.operations;

import com.commerceflow.mall.account.application.policy.OperatorAuthorizationPolicy;
import com.commerceflow.mall.api.CommerceApiContract;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned operator-only operations read model. */
@RestController
@RequestMapping(CommerceApiContract.V1_OPERATOR_OPERATIONS_PATH)
public class OperatorOperationsOverviewController {
    private final OperationsOverviewService service;
    private final OperatorAuthorizationPolicy operatorPolicy;

    public OperatorOperationsOverviewController(OperationsOverviewService service, OperatorAuthorizationPolicy operatorPolicy) {
        this.service = service;
        this.operatorPolicy = operatorPolicy;
    }

    @GetMapping(CommerceApiContract.OPERATIONS_OVERVIEW_PATH)
    public OperationsOverviewDto overview() {
        operatorPolicy.requireOperator();
        return service.overview();
    }
}
