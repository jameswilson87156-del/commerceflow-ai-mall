package com.commerceflow.mall.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "commerceflow.showcase.legacy-api-enabled=false",
        "commerceflow.ai.rate-limit.enabled=false"})
@AutoConfigureMockMvc
class LegacyApiPolicyTests {
    @Autowired MockMvc mockMvc;

    @Test
    void queryParameterUserScopedCompatibilityRoutesAreNotExposedWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/cart").param("userId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEGACY_ENDPOINT_DISABLED"));
    }
}
