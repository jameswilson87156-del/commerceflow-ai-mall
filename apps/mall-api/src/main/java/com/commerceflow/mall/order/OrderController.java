package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.account.application.policy.OperatorAuthorizationPolicy;
import com.commerceflow.mall.order.application.OrderReadService;
import com.commerceflow.mall.operations.LegacyApiPolicy;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CommerceApiContract.ORDERS_PATH)
public class OrderController {
    private final OrderService service; private final OrderReadService readService; private final LegacyApiPolicy legacyApi; private final OperatorAuthorizationPolicy operatorPolicy;
    public OrderController(OrderService service, OrderReadService readService, LegacyApiPolicy legacyApi, OperatorAuthorizationPolicy operatorPolicy) { this.service=service; this.readService=readService; this.legacyApi=legacyApi; this.operatorPolicy=operatorPolicy; }
    @PostMapping public ApiModels.OrderSummary submit(@RequestParam long userId, @RequestHeader(value="Idempotency-Key",required=false) String key, @Valid @RequestBody ApiModels.OrderRequest request) { legacyApi.requireEnabled(); return service.submit(userId,key,request); }
    @GetMapping public List<ApiModels.OrderSummary> list(@RequestParam long userId) { legacyApi.requireEnabled(); return readService.listForUser(userId); }
    @GetMapping(CommerceApiContract.ORDER_EXECUTION_EVIDENCE_PATH) public OrderExecutionEvidenceDto evidence(@PathVariable String orderNo) { legacyApi.requireEnabled(); operatorPolicy.requireOperator(); return readService.evidenceForOperator(orderNo); }
    @GetMapping(CommerceApiContract.ORDER_DETAIL_PATH) public ApiModels.OrderSummary detail(@PathVariable String orderNo) { legacyApi.requireEnabled(); operatorPolicy.requireOperator(); return readService.detailForOperator(orderNo); }
}
