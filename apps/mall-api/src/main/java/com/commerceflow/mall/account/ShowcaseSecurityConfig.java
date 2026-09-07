package com.commerceflow.mall.account;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Keeps explicit Showcase adapters in local/test mode. */
@Configuration
@ConditionalOnProperty(prefix = "commerceflow.auth", name = "mode", havingValue = "SHOWCASE", matchIfMissing = true)
public class ShowcaseSecurityConfig {
    @Bean
    SecurityFilterChain showcaseSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
