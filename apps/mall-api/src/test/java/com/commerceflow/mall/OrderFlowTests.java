package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.order.OrderEvidenceMapper;
import com.commerceflow.mall.order.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowTests {
    @Autowired OrderService service;
    @Autowired OrderEvidenceMapper evidenceMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;

    @Test
    void successfulOrderPersistsSnapshotsAmountAndMovementEvidence() {
        int before = stock(10002);
        String key = key("success");
        var first = service.submit(1, key, request(10002, 1));

        assertEquals("CREATED", first.status());
        assertEquals(0, first.totalAmount().compareTo(new BigDecimal("129.00")));
        assertEquals("轻盈棉质基础 T 恤", first.items().get(0).productNameSnapshot());
        assertEquals("T-SHIRT-WHITE-S", first.items().get(0).skuCodeSnapshot());
        assertEquals("白色", first.items().get(0).colorSnapshot());
        assertEquals("S", first.items().get(0).sizeSnapshot());
        assertEquals(before - 1, stock(10002));
        assertEquals(1, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo()));
        assertEquals(before, jdbc.queryForObject("SELECT stock_before FROM inventory_movement WHERE order_no=?", Integer.class, first.orderNo()));
        assertEquals(before - 1, jdbc.queryForObject("SELECT stock_after FROM inventory_movement WHERE order_no=?", Integer.class, first.orderNo()));
    }

    @Test
    void sameKeySameBodyReturnsOriginalWithoutAdditionalWrites() {
        String key = key("replay");
        int before = stock(10001);
        var first = service.submit(1, key, request(10001, 1));
        int itemsAfterFirst = count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", first.orderNo());
        int movementsAfterFirst = count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo());
        var replay = service.submit(1, key, request(10001, 1));

        assertEquals(first.orderNo(), replay.orderNo());
        assertEquals(before - 1, stock(10001));
        assertEquals(itemsAfterFirst, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", first.orderNo()));
        assertEquals(movementsAfterFirst, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo()));
    }

    @Test
    void insufficientInventoryLeavesNoOrderInventoryOrMovement() {
        String key = key("shortage");
        int before = stock(10005);
        assertThrows(CommerceException.class, () -> service.submit(1, key, request(10005, 1)));
        assertEquals(before, stock(10005));
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE idempotency_key=?", key));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement WHERE idempotency_key=?", key));
    }

    @Test
    void failureAfterAnEarlierDeductionRollsBackEverything() {
        String key = key("rollback");
        int shirtBefore = stock(10001);
        int outBefore = stock(10005);
        var mixedRequest = new ApiModels.OrderRequest(List.of(
            new ApiModels.OrderLineRequest(10001L, 1),
            new ApiModels.OrderLineRequest(10005L, 1)));

        assertThrows(CommerceException.class, () -> service.submit(1, key, mixedRequest));
        assertEquals(shirtBefore, stock(10001));
        assertEquals(outBefore, stock(10005));
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE idempotency_key=?", key));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement WHERE idempotency_key=?", key));
    }

    @Test
    void reusedKeyWithDifferentBodyReturns409AndDoesNotWrite() throws Exception {
        String key = key("conflict");
        var first = service.submit(1, key, request(10001, 1));
        int stockAfterFirst = stock(10001);
        int movementAfterFirst = count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo());

        mockMvc.perform(post("/api/orders")
                .param("userId", "1")
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content("{\"items\":[{\"skuId\":10001,\"quantity\":2}]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));

        assertEquals(1, count("SELECT COUNT(*) FROM orders WHERE idempotency_key=?", key));
        assertEquals(stockAfterFirst, stock(10001));
        assertEquals(movementAfterFirst, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo()));
    }

    @Test
    void executionEvidenceApiAndMybatisReadModelExposeStoredFacts() throws Exception {
        String key = key("evidence");
        var order = service.submit(1, key, request(10004, 2));

        var evidence = evidenceMapper.findByOrderNo(order.orderNo());
        assertEquals(order.orderNo(), evidence.getOrderNo());
        assertEquals(key, evidence.getIdempotencyKey());
        assertEquals("FIRST_CREATED", evidence.getRequestResult());
        assertEquals(1, evidence.getItems().size());
        assertEquals(1, evidence.getInventoryMovements().size());
        assertEquals(28, evidence.getInventoryMovements().get(0).getStockBefore());
        assertEquals(26, evidence.getInventoryMovements().get(0).getStockAfter());

        mockMvc.perform(get("/api/orders/{orderNo}/execution-evidence", order.orderNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNo").value(order.orderNo()))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.items[0].colorSnapshot").value("灰色"))
            .andExpect(jsonPath("$.inventoryMovements[0].stockBefore").value(28))
            .andExpect(jsonPath("$.inventoryMovements[0].stockAfter").value(26));
    }

    @Test
    void duplicateSkuLinesAreAggregatedIntoOneItemAndMovement() {
        String key = key("aggregate");
        int before = stock(10001);
        var request = new ApiModels.OrderRequest(List.of(
            new ApiModels.OrderLineRequest(10001L, 1),
            new ApiModels.OrderLineRequest(10001L, 2)));
        var order = service.submit(1, key, request);

        assertEquals(before - 3, stock(10001));
        assertEquals(1, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", order.orderNo()));
        assertEquals(1, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", order.orderNo()));
        assertEquals(3, jdbc.queryForObject("SELECT quantity FROM inventory_movement WHERE order_no=?", Integer.class, order.orderNo()));
    }

    @Test
    void databasePreventsDuplicateMovementForSameOrderSkuAndType() {
        var order = service.submit(1, key("unique"), request(10002, 1));
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
            "INSERT INTO inventory_movement(order_no,sku_id,movement_type,quantity,stock_before,stock_after,idempotency_key) VALUES (?,?,?,?,?,?,?)",
            order.orderNo(), 10002L, "ORDER_DEDUCT", 1, 1, 0, "manual-duplicate"));
    }

    @Test
    void cleanTestDatabaseAppliedTheEvidenceMigration() {
        assertEquals(6, count("SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE AND version IS NOT NULL"));
    }

    private ApiModels.OrderRequest request(long skuId, int quantity) {
        return new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(skuId, quantity)));
    }

    private int stock(long skuId) {
        return jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=?", Integer.class, skuId);
    }

    private int count(String sql, Object value) {
        return jdbc.queryForObject(sql, Integer.class, value);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private String key(String suffix) {
        return "p3-" + suffix + "-" + UUID.randomUUID();
    }
}
