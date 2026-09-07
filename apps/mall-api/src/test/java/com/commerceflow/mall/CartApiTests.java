package com.commerceflow.mall;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CartApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearDemoCart() {
        jdbc.update("DELETE FROM cart_item WHERE user_id=?", 1L);
        jdbc.update("UPDATE product SET status='ON_SALE' WHERE id IN (101,102)");
        jdbc.update("UPDATE product_sku SET status='ON_SALE' WHERE id IN (10001,10002,10003,10004,10005)");
    }

    @Test
    void addsReadsUpdatesAndDeletesARealServerCartItem() throws Exception {
        mockMvc.perform(post("/api/cart/items?userId=1")
                .contentType("application/json")
                .content("{\"skuId\":10004,\"quantity\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].skuId").value(10004))
            .andExpect(jsonPath("$[0].currency").value("CNY"))
            .andExpect(jsonPath("$[0].imagePath").value("/assets/products/product-tshirt-gray.png"));

        long itemId = jdbc.queryForObject("SELECT id FROM cart_item WHERE user_id=1 AND sku_id=10004", Long.class);
        mockMvc.perform(put("/api/cart/items/{itemId}?userId=1", itemId)
                .contentType("application/json")
                .content("{\"quantity\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].quantity").value(2));

        mockMvc.perform(delete("/api/cart/items/{itemId}?userId=1", itemId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cartItemUpdateAndDeleteAreScopedToTheDemoUser() throws Exception {
        mockMvc.perform(post("/api/cart/items?userId=1")
                .contentType("application/json")
                .content("{\"skuId\":10004,\"quantity\":1}"))
            .andExpect(status().isOk());
        long itemId = jdbc.queryForObject("SELECT id FROM cart_item WHERE user_id=1 AND sku_id=10004", Long.class);

        mockMvc.perform(put("/api/cart/items/{itemId}?userId=2", itemId)
                .contentType("application/json")
                .content("{\"quantity\":2}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/cart/items/{itemId}?userId=2", itemId))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidQuantityBeforeWritingTheCart() throws Exception {
        mockMvc.perform(post("/api/cart/items?userId=1")
                .contentType("application/json")
                .content("{\"skuId\":10004,\"quantity\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM cart_item WHERE user_id=1 AND sku_id=10004", Integer.class));
    }

    @Test
    void cannotAddAnOffSaleParentProductOrUnknownSkuToTheCart() throws Exception {
        jdbc.update("UPDATE product SET status='OFF_SALE' WHERE id=101");
        try {
            mockMvc.perform(post("/api/cart/items?userId=1")
                    .contentType("application/json")
                    .content("{\"skuId\":10004,\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
        } finally {
            jdbc.update("UPDATE product SET status='ON_SALE' WHERE id=101");
        }

        mockMvc.perform(post("/api/cart/items?userId=1")
                .contentType("application/json")
                .content("{\"skuId\":99999,\"quantity\":1}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }
}
