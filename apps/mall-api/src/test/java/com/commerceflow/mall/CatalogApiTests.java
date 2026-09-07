package com.commerceflow.mall;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void returnsShowcaseProductAndSkuImagePaths() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].productCode").value("PROD-1001"))
            .andExpect(jsonPath("$[0].coverImagePath").value("/assets/products/product-tshirt-white.png"))
            .andExpect(jsonPath("$[0].skus.length()").value(4))
            .andExpect(jsonPath("$[0].skus[0].imagePath").value("/assets/products/product-tshirt-black.png"))
            .andExpect(jsonPath("$[0].skus[2].availableStock").value(28))
            .andExpect(jsonPath("$[0].skus[3].availableStock").value(0));
    }

    @Test
    void returnsTheLocalizedProductDetailWithImageFields() throws Exception {
        mockMvc.perform(get("/api/products/102"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("简约通勤托特包"))
            .andExpect(jsonPath("$.productCode").value("PROD-1002"))
            .andExpect(jsonPath("$.coverImagePath").value("/assets/products/product-tote-beige.png"))
            .andExpect(jsonPath("$.skus[0].imagePath").value("/assets/products/product-tote-beige.png"));
    }

    @Test
    void hidesAnOffSaleParentProductFromTheDetailEndpoint() throws Exception {
        jdbc.update("UPDATE product SET status='OFF_SALE' WHERE id=102");
        try {
            mockMvc.perform(get("/api/products/102"))
                .andExpect(status().isNotFound());
        } finally {
            jdbc.update("UPDATE product SET status='ON_SALE' WHERE id=102");
        }
    }
}
