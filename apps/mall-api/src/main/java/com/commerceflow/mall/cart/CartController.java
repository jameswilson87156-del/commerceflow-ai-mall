package com.commerceflow.mall.cart;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
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
    @PutMapping("/items/{itemId}")
    public List<ApiModels.CartItem> update(@RequestParam(defaultValue="1") long userId, @PathVariable long itemId, @Valid @RequestBody ApiModels.CartQuantityRequest request) {
        if (repository.update(userId, itemId, request.quantity()) == 0) throw new CommerceException("CART_ITEM_NOT_FOUND", "Cart item not found");
        return repository.find(userId);
    }
    @DeleteMapping("/items/{itemId}")
    public List<ApiModels.CartItem> delete(@RequestParam(defaultValue="1") long userId, @PathVariable long itemId) {
        if (repository.delete(userId, itemId) == 0) throw new CommerceException("CART_ITEM_NOT_FOUND", "Cart item not found");
        return repository.find(userId);
    }
}
