package com.commerceflow.mall.order;

import com.commerceflow.mall.account.application.port.out.CurrentUserPort;
import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.order.application.OrderReadService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned order API that derives user ownership from CurrentUserPort. */
@RestController
@RequestMapping(CommerceApiContract.V1_ME_ORDERS_PATH)
public class ScopedOrderController {
    private final OrderService service;
    private final OrderReadService readService;
    private final CurrentUserPort currentUser;

    public ScopedOrderController(OrderService service, OrderReadService readService, CurrentUserPort currentUser) {
        this.service = service;
        this.readService = readService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ApiModels.OrderSummary submit(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @Valid @RequestBody ApiModels.OrderRequest request) {
        return service.submit(currentUser.currentUser().userId(), key, request);
    }

    @GetMapping
    public List<ApiModels.OrderSummary> list() {
        return readService.listForUser(currentUser.currentUser().userId());
    }

    @GetMapping("/{orderNo}/execution-evidence")
    public OrderExecutionEvidenceDto evidence(@PathVariable String orderNo) {
        return readService.evidenceForUser(currentUser.currentUser().userId(), orderNo);
    }

    @GetMapping("/{orderNo}")
    public ApiModels.OrderSummary detail(@PathVariable String orderNo) {
        return readService.detailForUser(currentUser.currentUser().userId(), orderNo);
    }
}
