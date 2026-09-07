package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.order.OrderEvidenceMapper;
import com.commerceflow.mall.order.OrderService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
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
    void successfulOrderPersistsRealProductIdImageSnapshotAmountAndMovementEvidence() {
        int before = stock(10002);
        var first = service.submit(1, key("success"), request(10002, 1));

        assertEquals("CREATED", first.status());
        assertEquals(0, first.totalAmount().compareTo(new BigDecimal("129.00")));
        assertEquals("T-SHIRT-WHITE-S", first.items().get(0).skuCodeSnapshot());
        assertEquals("/assets/products/product-tshirt-white.png", first.items().get(0).imagePathSnapshot());
        assertEquals(101L, jdbc.queryForObject("SELECT product_id FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", Long.class, first.orderNo()));
        assertEquals(10002L, jdbc.queryForObject("SELECT sku_id FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", Long.class, first.orderNo()));
        assertEquals(before - 1, stock(10002));
        assertEquals(1, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo()));
        assertEquals(before, jdbc.queryForObject("SELECT stock_before FROM inventory_movement WHERE order_no=?", Integer.class, first.orderNo()));
        assertEquals(before - 1, jdbc.queryForObject("SELECT stock_after FROM inventory_movement WHERE order_no=?", Integer.class, first.orderNo()));
    }

    @Test
    void sameKeySameBodyReturnsOriginalWithoutAdditionalWritesOrImageSnapshots() {
        String key = key("replay");
        int before = stock(10001);
        var first = service.submit(1, key, request(10001, 1));
        int itemsAfterFirst = count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", first.orderNo());
        int movementsAfterFirst = count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo());
        int snapshotsAfterFirst = count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) AND image_path_snapshot IS NOT NULL", first.orderNo());

        var replay = service.submit(1, key, request(10001, 1));

        assertEquals(first.orderNo(), replay.orderNo());
        assertEquals(before - 1, stock(10001));
        assertEquals(itemsAfterFirst, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", first.orderNo()));
        assertEquals(movementsAfterFirst, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", first.orderNo()));
        assertEquals(snapshotsAfterFirst, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) AND image_path_snapshot IS NOT NULL", first.orderNo()));
    }

    @Test
    void insufficientInventoryLeavesNoOrderItemImageSnapshotOrMovement() {
        String key = key("shortage");
        int before = stock(10005);

        assertThrows(CommerceException.class, () -> service.submit(1, key, request(10005, 1)));

        assertEquals(before, stock(10005));
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE idempotency_key=?", key));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movement WHERE idempotency_key=?", key));
        assertEquals(0, count("SELECT COUNT(*) FROM order_item oi JOIN orders o ON o.id=oi.order_id WHERE o.idempotency_key=?", key));
    }

    @Test
    void failureAfterAnEarlierDeductionRollsBackItemsImagesAndInventory() {
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
        assertEquals(0, count("SELECT COUNT(*) FROM order_item oi JOIN orders o ON o.id=oi.order_id WHERE o.idempotency_key=?", key));
    }

    @Test
    void reusedKeyWithDifferentBodyReturns409AndDoesNotWriteOrderItem() throws Exception {
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
        assertEquals(1, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", first.orderNo()));
    }

    @Test
    void evidenceApiAndMybatisReadModelExposeStoredImageSnapshot() throws Exception {
        var order = service.submit(1, key("evidence"), request(10004, 2));

        var evidence = evidenceMapper.findByOrderNo(order.orderNo());
        assertEquals(order.orderNo(), evidence.getOrderNo());
        assertEquals(1, evidence.getItems().size());
        assertEquals(1, evidence.getInventoryMovements().size());
        assertEquals("/assets/products/product-tshirt-gray.png", evidence.getItems().get(0).getImagePathSnapshot());

        mockMvc.perform(get("/api/orders/{orderNo}/execution-evidence", order.orderNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].imagePathSnapshot").value("/assets/products/product-tshirt-gray.png"));
    }

    @Test
    void duplicateSkuLinesAreAggregatedIntoOneItemMovementAndImageSnapshot() {
        int before = stock(10001);
        var request = new ApiModels.OrderRequest(List.of(
            new ApiModels.OrderLineRequest(10001L, 1),
            new ApiModels.OrderLineRequest(10001L, 2)));
        var order = service.submit(1, key("aggregate"), request);

        assertEquals(before - 3, stock(10001));
        assertEquals(1, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", order.orderNo()));
        assertEquals(1, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", order.orderNo()));
        assertEquals(1, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) AND image_path_snapshot IS NOT NULL", order.orderNo()));
    }

    @Test
    void databasePreventsDuplicateMovementForSameOrderSkuAndType() {
        var order = service.submit(1, key("unique"), request(10002, 1));
        assertThrows(DuplicateKeyException.class, () -> jdbc.update(
            "INSERT INTO inventory_movement(order_no,sku_id,movement_type,quantity,stock_before,stock_after,idempotency_key) VALUES (?,?,?,?,?,?,?)",
            order.orderNo(), 10002L, "ORDER_DEDUCT", 1, 1, 0, "manual-duplicate"));
    }

    @Test
    void rapidOrdersReceiveDistinctOrderNumbersWithoutChangingIdempotencyBehavior() {
        var first = service.submit(1, key("rapid-one"), request(10001, 1));
        var second = service.submit(1, key("rapid-two"), request(10002, 1));

        assertNotEquals(first.orderNo(), second.orderNo());
        assertEquals(34, first.orderNo().length());
        assertEquals(34, second.orderNo().length());
    }

    @Test
    void dualProductOrderKeepsTwoItemsTwoMovementsAndTwoImageSnapshotsInMybatisReadModel() throws Exception {
        var request = new ApiModels.OrderRequest(List.of(
            new ApiModels.OrderLineRequest(10004L, 1),
            new ApiModels.OrderLineRequest(10003L, 1)));
        var order = service.submit(1, key("dual-image"), request);
        var evidence = evidenceMapper.findByOrderNo(order.orderNo());

        assertEquals(0, order.totalAmount().compareTo(new BigDecimal("328.00")));
        assertEquals(2, order.items().size());
        assertEquals(2, count("SELECT COUNT(*) FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", order.orderNo()));
        assertEquals(2, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", order.orderNo()));
        assertEquals(101L, jdbc.queryForObject("SELECT product_id FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) AND sku_id=10004", Long.class, order.orderNo()));
        assertEquals(102L, jdbc.queryForObject("SELECT product_id FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) AND sku_id=10003", Long.class, order.orderNo()));
        assertEquals(2, evidence.getItems().size());
        assertEquals(2, evidence.getInventoryMovements().size());
        assertEquals(2, evidence.getItems().stream().map(item -> item.getSkuCodeSnapshot()).distinct().count());
        assertEquals(2, evidence.getInventoryMovements().stream().map(movement -> movement.getSkuId()).distinct().count());
        var gray = evidence.getItems().stream().filter(item -> item.getSkuCodeSnapshot().equals("T-SHIRT-GRAY-L")).findFirst().orElseThrow();
        var tote = evidence.getItems().stream().filter(item -> item.getSkuCodeSnapshot().equals("TOTE-BEIGE-ONE")).findFirst().orElseThrow();
        assertEquals("/assets/products/product-tshirt-gray.png", gray.getImagePathSnapshot());
        assertEquals("/assets/products/product-tote-beige.png", tote.getImagePathSnapshot());
        assertEquals(0, gray.getSubtotal().compareTo(new BigDecimal("129.00")));
        assertEquals(0, tote.getSubtotal().compareTo(new BigDecimal("199.00")));

        mockMvc.perform(get("/api/orders/{orderNo}", order.orderNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].imagePathSnapshot").value("/assets/products/product-tshirt-gray.png"))
            .andExpect(jsonPath("$.items[1].imagePathSnapshot").value("/assets/products/product-tote-beige.png"));
        mockMvc.perform(get("/api/orders/{orderNo}/execution-evidence", order.orderNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.inventoryMovements.length()").value(2))
            .andExpect(jsonPath("$.items[0].imagePathSnapshot").value("/assets/products/product-tshirt-gray.png"))
            .andExpect(jsonPath("$.items[1].imagePathSnapshot").value("/assets/products/product-tote-beige.png"));
    }

    @Test
    void multiSkuOrderPreservesRequestOrderInStoredSnapshots() {
        var request = new ApiModels.OrderRequest(List.of(
                new ApiModels.OrderLineRequest(10004L, 1),
                new ApiModels.OrderLineRequest(10003L, 1)));

        var order = service.submit(1, key("lock-order"), request);

        assertEquals(10004L, jdbc.queryForObject(
                "SELECT sku_id FROM inventory_movement WHERE order_no=? ORDER BY id LIMIT 1",
                Long.class,
                order.orderNo()));
        assertEquals(10004L, jdbc.queryForObject(
                "SELECT sku_id FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) ORDER BY id LIMIT 1",
                Long.class,
                order.orderNo()));
        assertEquals(10003L, jdbc.queryForObject(
                "SELECT sku_id FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) ORDER BY id LIMIT 1 OFFSET 1",
                Long.class,
                order.orderNo()));
    }

    @Test
    void legacyOrderWithNullImageSnapshotRemainsReadable() {
        String orderNo = "LEGACY" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO orders(order_no,user_id,idempotency_key,request_fingerprint,total_amount,currency,status) VALUES (?,?,?,?,?,?,?)", orderNo, 1L, key("legacy"), "legacy-fingerprint", new BigDecimal("129.00"), "CNY", "CREATED");
        long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE order_no=?", Long.class, orderNo);
        jdbc.update("INSERT INTO order_item(order_id,product_id,sku_id,product_name_snapshot,sku_code_snapshot,sku_attributes_snapshot,color_snapshot,size_snapshot,unit_price,quantity) VALUES (?,?,?,?,?,?,?,?,?,?)", orderId, 101L, 10002L, "Legacy shirt", "T-SHIRT-WHITE-S", "white / S", "white", "S", new BigDecimal("129.00"), 1);

        var evidence = evidenceMapper.findByOrderNo(orderNo);
        assertEquals(1, evidence.getItems().size());
        assertNull(evidence.getItems().get(0).getImagePathSnapshot());
    }

    @Test
    void nestedOrderLineValidationRejectsZeroQuantityBeforeAnyTransactionWrite() throws Exception {
        String key = key("nested-validation");
        int before = stock(10004);

        mockMvc.perform(post("/api/orders")
                .param("userId", "1")
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content("{\"items\":[{\"skuId\":10004,\"quantity\":0}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertEquals(before, stock(10004));
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE idempotency_key=?", key));
    }

    @Test
    void orderRequiresBoundedIdempotencyKeyAndRejectsMissingOrOversizedValues() throws Exception {
        mockMvc.perform(post("/api/orders")
                .param("userId", "1")
                .contentType("application/json")
                .content("{\"items\":[{\"skuId\":10004,\"quantity\":1}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));

        mockMvc.perform(post("/api/orders")
                .param("userId", "1")
                .header("Idempotency-Key", "x".repeat(121))
                .contentType("application/json")
                .content("{\"items\":[{\"skuId\":10004,\"quantity\":1}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_INVALID"));
    }

    @Test
    void unknownUserCannotCreateAnOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                .param("userId", "99999")
                .header("Idempotency-Key", key("unknown-user"))
                .contentType("application/json")
                .content("{\"items\":[{\"skuId\":10004,\"quantity\":1}]}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void anOffSaleParentProductCannotBeOrderedThroughItsOnSaleSku() throws Exception {
        String key = key("off-sale-parent");
        int before = stock(10004);
        jdbc.update("UPDATE product SET status='OFF_SALE' WHERE id=101");
        try {
            mockMvc.perform(post("/api/orders")
                    .param("userId", "1")
                    .header("Idempotency-Key", key)
                    .contentType("application/json")
                    .content("{\"items\":[{\"skuId\":10004,\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
        } finally {
            jdbc.update("UPDATE product SET status='ON_SALE' WHERE id=101");
        }
        assertEquals(before, stock(10004));
        assertEquals(0, count("SELECT COUNT(*) FROM orders WHERE idempotency_key=?", key));
    }

    @Test
    void concurrentDifferentIdempotencyKeysNeverCreateNegativeStock() throws Exception {
        int requestCount = 12;
        int before = stock(10004);
        var pool = Executors.newFixedThreadPool(requestCount);
        try {
            List<Callable<ApiModels.OrderSummary>> tasks = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                int requestIndex = index;
                tasks.add(() -> service.submit(1, key("concurrent-" + requestIndex), request(10004, 1)));
            }
            long successCount = pool.invokeAll(tasks).stream().map(future -> {
                try {
                    future.get();
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }).filter(Boolean::booleanValue).count();
            assertEquals(requestCount, successCount);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(before - requestCount, stock(10004));
        assertTrue(stock(10004) >= 0);
    }

    @Test
    void cleanTestDatabaseAppliedAllMigrationsThroughCnyContract() {
        assertEquals(11, count("SELECT COUNT(*) FROM flyway_schema_history WHERE success=TRUE AND version IS NOT NULL"));
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
        return "p31-" + suffix + "-" + UUID.randomUUID();
    }
}
