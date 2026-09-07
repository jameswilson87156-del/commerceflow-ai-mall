package com.commerceflow.mall.cart;

import com.commerceflow.mall.account.application.port.out.CurrentUserPort;
import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.core.CommerceException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned cart API whose user scope is derived on the server. */
@RestController
@RequestMapping(CommerceApiContract.V1_ME_CART_PATH)
public class ScopedCartController {
    private final CartRepository repository;
    private final CurrentUserPort currentUser;

    public ScopedCartController(CartRepository repository, CurrentUserPort currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ApiModels.CartItem> get() {
        return repository.find(currentUser.currentUser().userId());
    }

    @PostMapping(CommerceApiContract.CART_ITEMS_PATH)
    public List<ApiModels.CartItem> add(@Valid @RequestBody ApiModels.CartItemRequest request) {
        long userId = currentUser.currentUser().userId();
        repository.add(userId, request.skuId(), request.quantity());
        return repository.find(userId);
    }

    @PutMapping(CommerceApiContract.CART_ITEMS_PATH + "/{itemId}")
    public List<ApiModels.CartItem> update(@PathVariable long itemId, @Valid @RequestBody ApiModels.CartQuantityRequest request) {
        long userId = currentUser.currentUser().userId();
        if (repository.update(userId, itemId, request.quantity()) == 0) {
            throw new CommerceException("CART_ITEM_NOT_FOUND", "Cart item not found");
        }
        return repository.find(userId);
    }

    @DeleteMapping(CommerceApiContract.CART_ITEMS_PATH + "/{itemId}")
    public List<ApiModels.CartItem> delete(@PathVariable long itemId) {
        long userId = currentUser.currentUser().userId();
        if (repository.delete(userId, itemId) == 0) {
            throw new CommerceException("CART_ITEM_NOT_FOUND", "Cart item not found");
        }
        return repository.find(userId);
    }
}
