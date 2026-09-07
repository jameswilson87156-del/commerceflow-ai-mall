package com.commerceflow.mall.account;

import com.commerceflow.mall.account.application.port.out.CurrentUserPort;
import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versioned current-user endpoint; its identity is server-derived. */
@RestController
@RequestMapping(CommerceApiContract.V1_ME_PATH)
public class CurrentUserController {
    private final CurrentUserPort currentUser;

    public CurrentUserController(CurrentUserPort currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping
    public ApiModels.LoginResponse me() {
        var scope = currentUser.currentUser();
        return new ApiModels.LoginResponse(scope.userId(), scope.username(), scope.displayName());
    }
}
