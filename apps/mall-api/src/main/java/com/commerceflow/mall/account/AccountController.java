package com.commerceflow.mall.account;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.operations.LegacyApiPolicy;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CommerceApiContract.AUTH_PATH)
public class AccountController {
    private final JdbcTemplate jdbc;
    private final LegacyApiPolicy legacyApi;
    public AccountController(JdbcTemplate jdbc, LegacyApiPolicy legacyApi) { this.jdbc = jdbc; this.legacyApi = legacyApi; }
    @PostMapping(CommerceApiContract.AUTH_DEMO_LOGIN_PATH)
    public ApiModels.LoginResponse login(@Valid @RequestBody ApiModels.LoginRequest request) {
        legacyApi.requireEnabled();
        return jdbc.queryForObject("SELECT id,username,display_name FROM user_account WHERE username=? AND status='ACTIVE'", (rs, n) -> new ApiModels.LoginResponse(rs.getLong("id"), rs.getString("username"), rs.getString("display_name")), request.username());
    }
}
