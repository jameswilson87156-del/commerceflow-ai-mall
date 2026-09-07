package com.commerceflow.mall.account;

import com.commerceflow.mall.core.SecurityJsonHandlers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/** Stateless JWT resource-server mode for staging and production. */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "commerceflow.auth", name = "mode", havingValue = "OIDC")
public class OidcSecurityConfig {
    private final ExternalAuthProperties properties;

    public OidcSecurityConfig(ExternalAuthProperties properties) {
        this.properties = properties;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        String issuer = properties.getIssuerUri();
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("COMMERCEFLOW_AUTH_ISSUER_URI must be configured when OIDC auth is enabled.");
        }
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        String audience = properties.getAudience();
        if (audience == null || audience.isBlank()) return decoder;
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing.", null));
        if (!(decoder instanceof NimbusJwtDecoder nimbusDecoder)) {
            throw new IllegalStateException("OIDC decoder does not support audience validation.");
        }
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return nimbusDecoder;
    }

    @Bean
    SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/products", "/api/products/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(SecurityJsonHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(SecurityJsonHandlers.accessDeniedHandler()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter())));
        return http.build();
    }
}
