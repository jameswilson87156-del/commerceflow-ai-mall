package com.commerceflow.mall.account;

import com.commerceflow.mall.api.ApiModels;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AccountController {
    private final JdbcTemplate jdbc;
    public AccountController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostMapping("/demo-login")
    public ApiModels.LoginResponse login(@Valid @RequestBody ApiModels.LoginRequest request) {
        return jdbc.queryForObject("SELECT id,username,display_name FROM user_account WHERE username=? AND status='ACTIVE'", (rs, n) -> new ApiModels.LoginResponse(rs.getLong("id"), rs.getString("username"), rs.getString("display_name")), request.username());
    }
}
