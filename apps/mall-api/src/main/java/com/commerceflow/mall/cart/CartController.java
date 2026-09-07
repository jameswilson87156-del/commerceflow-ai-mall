package com.commerceflow.mall.cart;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.operations.LegacyApiPolicy;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CommerceApiContract.CART_PATH)
public class CartController {
    private final CartRepository repository;
    private final LegacyApiPolicy legacyApi;
    public CartController(CartRepository repository, LegacyApiPolicy legacyApi) { this.repository = repository; this.legacyApi = legacyApi; }
    @GetMapping public List<ApiModels.CartItem> get(@RequestParam long userId) { legacyApi.requireEnabled(); return repository.find(userId); }
    @PostMapping(CommerceApiContract.CART_ITEMS_PATH) public List<ApiModels.CartItem> add(@RequestParam long userId, @Valid @RequestBody ApiModels.CartItemRequest request) { legacyApi.requireEnabled(); repository.add(userId, request.skuId(), request.quantity()); return repository.find(userId); }
    @PutMapping(CommerceApiContract.CART_ITEMS_PATH + "/{itemId}")
    public List<ApiModels.CartItem> update(@RequestParam long userId, @PathVariable long itemId, @Valid @RequestBody ApiModels.CartQuantityRequest request) {
        legacyApi.requireEnabled();
        if (repository.update(userId, itemId, request.quantity()) == 0) throw new CommerceException("CART_ITEM_NOT_FOUND", "Cart item not found");
        return repository.find(userId);
    }
    @DeleteMapping(CommerceApiContract.CART_ITEMS_PATH + "/{itemId}")
    public List<ApiModels.CartItem> delete(@RequestParam long userId, @PathVariable long itemId) {
        legacyApi.requireEnabled();
        if (repository.delete(userId, itemId) == 0) throw new CommerceException("CART_ITEM_NOT_FOUND", "Cart item not found");
        return repository.find(userId);
    }
}
