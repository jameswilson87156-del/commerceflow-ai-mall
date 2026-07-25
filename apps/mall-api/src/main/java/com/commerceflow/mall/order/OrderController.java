package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service; private final OrderRepository repository;
    public OrderController(OrderService service, OrderRepository repository) { this.service=service; this.repository=repository; }
    @PostMapping public ApiModels.OrderSummary submit(@RequestParam(defaultValue="1") long userId, @RequestHeader(value="Idempotency-Key",required=false) String key, @Valid @RequestBody ApiModels.OrderRequest request) { return service.submit(userId,key,request); }
    @GetMapping public List<ApiModels.OrderSummary> list(@RequestParam(defaultValue="1") long userId) { return repository.findAll(userId); }
    @GetMapping("/{orderNo}") public ApiModels.OrderSummary detail(@PathVariable String orderNo) { return repository.find(orderNo).orElseThrow(() -> new com.commerceflow.mall.core.CommerceException("ORDER_NOT_FOUND", "Order not found")); }
}
