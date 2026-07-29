package com.commerceflow.mall;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OperationsOverviewApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetShowcaseFacts() {
        jdbc.update("DELETE FROM inventory_movement");
        jdbc.update("DELETE FROM order_item");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM cart_item");
        jdbc.update("DELETE FROM ai_trace");
        jdbc.update("UPDATE inventory SET available_stock = CASE sku_id "
                + "WHEN 10001 THEN 96 WHEN 10002 THEN 182 WHEN 10003 THEN 128 "
                + "WHEN 10004 THEN 28 WHEN 10005 THEN 0 END "
                + "WHERE sku_id IN (10001, 10002, 10003, 10004, 10005)");
    }

    @Test
    void returnsOnlyDatabaseAndConfigurationBackedShowcaseFacts() throws Exception {
        mockMvc.perform(get("/api/operations/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.productCount").value(2))
            .andExpect(jsonPath("$.summary.skuCount").value(5))
            .andExpect(jsonPath("$.summary.onSaleProductCount").value(2))
            .andExpect(jsonPath("$.summary.availableStockTotal").isNumber())
            .andExpect(jsonPath("$.summary.createdOrderCount").value(0))
            .andExpect(jsonPath("$.summary.createdOrderAmount").value(0))
            .andExpect(jsonPath("$.recentOrders").isArray())
            .andExpect(jsonPath("$.lowStockSkus[0].stockLevel").value("OUT_OF_STOCK"))
            .andExpect(jsonPath("$.runtimeBoundary.dataScope").value("LOCAL_SHOWCASE"))
            .andExpect(jsonPath("$.runtimeBoundary.aiMode").value("MOCK"))
            .andExpect(jsonPath("$.runtimeBoundary.rateLimitLimit").value(5))
            .andExpect(jsonPath("$.runtimeBoundary.orderStatusScope").value("CREATED_ONLY"));
    }

    @Test
    void emptyTraceReturnsZeroAndNoSensitiveRuntimeFields() throws Exception {
        mockMvc.perform(get("/api/operations/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiSummary.interactionCount").value(0))
            .andExpect(jsonPath("$.aiSummary.provider").value("NONE"))
            .andExpect(jsonPath("$.runtimeBoundary.identityHashSecret").doesNotExist())
            .andExpect(jsonPath("$.runtimeBoundary.redisHost").doesNotExist());
    }
}
