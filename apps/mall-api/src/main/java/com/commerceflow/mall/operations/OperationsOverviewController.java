package com.commerceflow.mall.operations;

import com.commerceflow.mall.account.application.policy.OperatorAuthorizationPolicy;
import com.commerceflow.mall.api.CommerceApiContract;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CommerceApiContract.OPERATIONS_PATH)
public class OperationsOverviewController {
    private final OperationsOverviewService service;
    private final LegacyApiPolicy legacyApi;
    private final OperatorAuthorizationPolicy operatorPolicy;
    public OperationsOverviewController(OperationsOverviewService service, LegacyApiPolicy legacyApi, OperatorAuthorizationPolicy operatorPolicy) {
        this.service = service;
        this.legacyApi = legacyApi;
        this.operatorPolicy = operatorPolicy;
    }
    @GetMapping(CommerceApiContract.OPERATIONS_OVERVIEW_PATH)
    public OperationsOverviewDto overview() {
        legacyApi.requireEnabled();
        operatorPolicy.requireOperator();
        return service.overview();
    }
}
