package com.commerceflow.mall;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerceflow.mall.ai.AiModels;
import com.commerceflow.mall.ai.CustomerServiceProviderClient;
import com.commerceflow.mall.operations.ShowcaseRuntimeProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** HTTP regression tests for the Phase 0-B consumer/operator boundary. */
@SpringBootTest
@AutoConfigureMockMvc
class Phase0BBoundaryApiTests {
    private static final long FIXTURE_USER_ID = 200002L;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ShowcaseRuntimeProperties runtime;
    @MockitoBean CustomerServiceProviderClient providerClient;

    private List<CartRow> originalUserCart = List.of();
    private boolean fixtureUserCreated;

    @BeforeEach
    void setUpExplicitDemoFixture() {
        reset(providerClient);
        configureDemoRuntime();
        originalUserCart = jdbc.query(
                "SELECT sku_id, quantity FROM cart_item WHERE user_id=1 ORDER BY sku_id",
                (rs, rowNum) -> new CartRow(rs.getLong("sku_id"), rs.getInt("quantity")));
        jdbc.update("DELETE FROM cart_item WHERE user_id IN (1, ?)", FIXTURE_USER_ID);
        ensureFixtureUser();
        jdbc.update("DELETE FROM cart_item WHERE user_id=?", FIXTURE_USER_ID);
        jdbc.update("INSERT INTO cart_item(user_id, sku_id, quantity) VALUES (1, 10001, 1)");
        jdbc.update("INSERT INTO cart_item(user_id, sku_id, quantity) VALUES (?, 10002, 2)", FIXTURE_USER_ID);
        cleanupFixtureOrders();
        jdbc.update("DELETE FROM ai_trace WHERE client_request_id LIKE 'p0b-%'");
        when(providerClient.answer(any())).thenAnswer(invocation -> {
            AiModels.PythonCustomerServiceRequest request = invocation.getArgument(0);
            return new AiModels.PythonCustomerServiceResponse(
                    request.traceId(),
                    "灰色 L 码当前库存为 28 件，可以购买，售价为 ¥129.00。",
                    AiModels.AnswerStatus.ANSWERED,
                    new AiModels.Provider("commerceflow-mock", AiModels.ProviderMode.MOCK, null),
                    null);
        });
    }

    @AfterEach
    void tearDownFixtureAndRestoreExplicitTestMode() {
        cleanupFixtureOrders();
        jdbc.update("DELETE FROM ai_trace WHERE client_request_id LIKE 'p0b-%'");
        jdbc.update("DELETE FROM cart_item WHERE user_id IN (1, ?)", FIXTURE_USER_ID);
        for (CartRow row : originalUserCart) {
            jdbc.update("INSERT INTO cart_item(user_id, sku_id, quantity) VALUES (1, ?, ?)", row.skuId(), row.quantity());
        }
        if (fixtureUserCreated) {
            jdbc.update("DELETE FROM user_account WHERE id=?", FIXTURE_USER_ID);
        }
        configureDemoRuntime();
    }

    @Test
    void consumerCartAndOrdersIgnoreClientUserIdAndUseTheServerOwnedUserScope() throws Exception {
        String otherUserOrder = insertOrder(FIXTURE_USER_ID, "other-user");
        String currentUserOrder = insertOrder(1L, "current-user");

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
        mockMvc.perform(get("/api/v1/me/cart").param("userId", String.valueOf(FIXTURE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuId").value(10001))
                .andExpect(jsonPath("$[0].quantity").value(1));

        mockMvc.perform(get("/api/v1/me/orders").param("userId", String.valueOf(FIXTURE_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNo").value(currentUserOrder));

        mockMvc.perform(get("/api/v1/me/orders/{orderNo}", otherUserOrder)
                        .param("userId", String.valueOf(FIXTURE_USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void consumerAiDerivesUserIdAndAcceptsOnlyTheVersionedRequestShape() throws Exception {
        mockMvc.perform(post("/api/v1/me/ai/customer-service/ask")
                        .contentType("application/json")
                        .content("{\"productId\":101,\"skuId\":10004,\"question\":\"库存还有吗？\",\"clientRequestId\":\"p0b-ai-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerStatus").value("ANSWERED"))
                .andExpect(jsonPath("$.businessFacts.productId").value(101));

        Long storedUserId = jdbc.queryForObject(
                "SELECT user_id FROM ai_trace WHERE client_request_id='p0b-ai-001'",
                Long.class);
        org.junit.jupiter.api.Assertions.assertEquals(1L, storedUserId);
    }

    @Test
    void consumerScopeFailsClosedOutsideExplicitDemoIdentityMode() throws Exception {
        configureStagingRuntime();

        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/me/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/me/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(post("/api/v1/me/ai/customer-service/ask")
                        .contentType("application/json")
                        .content("{\"productId\":101,\"skuId\":10004,\"question\":\"库存还有吗？\",\"clientRequestId\":\"p0b-ai-unauthorized\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void operatorRoutesAllowCrossUserReadsOnlyWithAnExplicitOperatorIdentity() throws Exception {
        insertOrder(FIXTURE_USER_ID, "operator-read");

        mockMvc.perform(get("/api/v1/operator/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value((int) FIXTURE_USER_ID));
        mockMvc.perform(get("/api/v1/operator/operations/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runtimeBoundary.authenticationMode").value("DEMO_USER"));

        runtime.setOperatorAuthenticationMode("DEMO_USER");
        mockMvc.perform(get("/api/v1/operator/orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OPERATOR_FORBIDDEN"));
    }

    @Test
    void stagingFailsClosedForOperatorRoutesAndDisablesLegacyOperationsRoute() throws Exception {
        configureStagingRuntime();

        mockMvc.perform(get("/api/v1/operator/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("OPERATOR_UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/operator/operations/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("OPERATOR_UNAUTHENTICATED"));
        mockMvc.perform(get("/api/operations/overview"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEGACY_ENDPOINT_DISABLED"));
    }

    @Test
    void legacyQueryUserIdRouteRequiresAnExplicitParameterAndDoesNotSupplyADefaultIdentity() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private void configureDemoRuntime() {
        runtime.setMode("DEMO");
        runtime.setDataScope("LOCAL_SHOWCASE");
        runtime.setAuthenticationMode("DEMO_USER");
        runtime.setDemoUserId(1L);
        runtime.setDemoUsername("demo@commerceflow.local");
        runtime.setDemoDisplayName("Demo Shopper");
        runtime.setOperatorAuthenticationMode("DEMO_OPERATOR");
        runtime.setDemoOperatorId(9001L);
        runtime.setDemoOperatorUsername("operator@commerceflow.local");
        runtime.setDemoOperatorDisplayName("Showcase Operator");
        runtime.setLegacyApiEnabled(true);
    }

    private void configureStagingRuntime() {
        runtime.setMode("STAGING");
        runtime.setDataScope("STAGING");
        runtime.setAuthenticationMode("EXTERNAL");
        runtime.setDemoUserId(0L);
        runtime.setOperatorAuthenticationMode("EXTERNAL");
        runtime.setDemoOperatorId(0L);
        runtime.setLegacyApiEnabled(true);
    }

    private void ensureFixtureUser() {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE id=?", Integer.class, FIXTURE_USER_ID) == 0) {
            jdbc.update("INSERT INTO user_account(id, username, display_name, status) VALUES (?, ?, ?, 'ACTIVE')",
                    FIXTURE_USER_ID, "p0b-user@commerceflow.local", "Phase 0-B User");
            fixtureUserCreated = true;
        }
    }

    private String insertOrder(long userId, String suffix) {
        String orderNo = "P0B-" + UUID.randomUUID().toString().replace("-", "");
        String key = "p0b-" + suffix + "-" + UUID.randomUUID();
        jdbc.update("INSERT INTO orders(order_no,user_id,idempotency_key,request_fingerprint,total_amount,currency,status) VALUES (?,?,?,?,?,?,?)",
                orderNo, userId, key, "phase0b-" + suffix, new BigDecimal("129.00"), "CNY", "CREATED");
        long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_no=?", Long.class, orderNo);
        jdbc.update("INSERT INTO order_item(order_id,product_id,sku_id,product_name_snapshot,sku_code_snapshot,sku_attributes_snapshot,color_snapshot,size_snapshot,image_path_snapshot,unit_price,quantity) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                orderId, 101L, 10001L, "Phase 0-B Shirt", "SHIRT-BLK-M", "Black / M", "Black", "M",
                "/assets/products/product-tshirt-black.png", new BigDecimal("129.00"), 1);
        return orderNo;
    }

    private void cleanupFixtureOrders() {
        jdbc.update("DELETE FROM inventory_movement WHERE order_no LIKE 'P0B-%'");
        jdbc.update("DELETE FROM order_item WHERE order_id IN (SELECT id FROM orders WHERE order_no LIKE 'P0B-%')");
        jdbc.update("DELETE FROM orders WHERE order_no LIKE 'P0B-%'");
    }

    private record CartRow(long skuId, int quantity) {
    }
}
