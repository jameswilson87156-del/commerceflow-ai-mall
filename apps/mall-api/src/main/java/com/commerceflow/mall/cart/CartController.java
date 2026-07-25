package com.commerceflow.mall.cart;

import com.commerceflow.mall.api.ApiModels;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartRepository repository;
    public CartController(CartRepository repository) { this.repository = repository; }
    @GetMapping public List<ApiModels.CartItem> get(@RequestParam(defaultValue="1") long userId) { return repository.find(userId); }
    @PostMapping("/items") public List<ApiModels.CartItem> add(@RequestParam(defaultValue="1") long userId, @Valid @RequestBody ApiModels.CartItemRequest request) { repository.add(userId, request.skuId(), request.quantity()); return repository.find(userId); }
}
