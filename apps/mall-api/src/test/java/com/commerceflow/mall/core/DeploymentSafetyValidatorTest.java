package com.commerceflow.mall.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DeploymentSafetyValidatorTest {

    @Test
    void ignoresLocalProfiles() {
        assertThatCode(() -> new DeploymentSafetyValidator(new MockEnvironment()).validateWhenDeploying())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDemoRateLimitSecretInDeploymentProfiles() {
        MockEnvironment environment = validDeploymentEnvironment();
        environment.setProperty("commerceflow.ai.rate-limit.identity-hash-secret", "local-showcase-rate-limit-salt");

        assertThatIllegalStateException()
                .isThrownBy(() -> new DeploymentSafetyValidator(environment).validateWhenDeploying())
                .withMessageContaining("identity-hash-secret");
    }

    @Test
    void acceptsCompleteExternalDeploymentConfiguration() {
        assertThatCode(() -> new DeploymentSafetyValidator(validDeploymentEnvironment()).validateWhenDeploying())
                .doesNotThrowAnyException();
    }

    private MockEnvironment validDeploymentEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        return environment
                .withProperty("commerceflow.auth.mode", "OIDC")
                .withProperty("commerceflow.auth.issuer-uri", "https://id.example.test/realms/mall")
                .withProperty("commerceflow.showcase.authentication-mode", "EXTERNAL")
                .withProperty("commerceflow.showcase.operator-authentication-mode", "EXTERNAL")
                .withProperty("commerceflow.showcase.legacy-api-enabled", "false")
                .withProperty("commerceflow.ai.provider.fallback-enabled", "false")
                .withProperty("commerceflow.ai.rate-limit.enabled", "true")
                .withProperty("commerceflow.ai.rate-limit.failure-policy", "FAIL_CLOSED")
                .withProperty("commerceflow.ai.rate-limit.identity-hash-secret", "staging-hmac-secret")
                .withProperty("spring.datasource.url", "jdbc:mysql://mysql:3306/commerceflow")
                .withProperty("spring.datasource.username", "commerceflow")
                .withProperty("spring.datasource.password", "database-password")
                .withProperty("commerceflow.ai.provider.mode", "openai-compatible")
                .withProperty("commerceflow.ai.provider.base-url", "https://provider.example.test/v1")
                .withProperty("commerceflow.ai.provider.model", "model")
                .withProperty("commerceflow.ai.provider.api-key", "provider-key")
                .withProperty("commerceflow.cors.allowed-origins", "https://admin.example.test,https://mall.example.test");
    }
}
