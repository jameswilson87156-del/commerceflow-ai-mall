package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.order.OrderService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Phase O1 proof suite. It is deliberately disabled unless the isolated MySQL runner enables it.
 * No H2 result from this class is accepted as concurrency evidence.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ORDER_RELIABILITY_MYSQL_ENABLED", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderReliabilityMySqlTests {
    private static final Map<String, String> RESULTS = new LinkedHashMap<>();

    @Autowired OrderService orders;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> value("ORDER_RELIABILITY_DB_URL", "jdbc:h2:mem:order_reliability_disabled"));
        registry.add("spring.datasource.username", () -> value("ORDER_RELIABILITY_DB_USERNAME", "sa"));
        registry.add("spring.datasource.password", () -> value("ORDER_RELIABILITY_DB_PASSWORD", ""));
        registry.add("commerceflow.ai.rate-limit.enabled", () -> "false");
    }

    @Test
    @Order(1)
    void inventoryCompetitionUsesConditionalUpdateWithoutNegativeStock() throws Exception {
        assertMySql84();
        Fixture fixture = fixture(10, null);
        List<Attempt> attempts = concurrently(50, index -> orders.submit(
                fixture.userId(), key("competition", index), request(fixture.skuA(), 1)));

        long successful = attempts.stream().filter(Attempt::successful).count();
        long insufficient = attempts.stream().filter(attempt -> attempt.hasCode("INVENTORY_INSUFFICIENT")).count();
        assertEquals(10, successful, "exactly the available ten requests create orders");
        assertEquals(40, insufficient, "the remaining requests fail because stock is exhausted");
        assertEquals(0, stock(fixture.skuA()));
        assertEquals(10, orderCount(fixture.userId()));
        assertEquals(10, itemCount(fixture.userId()));
        assertEquals(10, movementCount(fixture.userId()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM inventory WHERE sku_id=? AND available_stock<0", Integer.class, fixture.skuA()));

        List<StockTransition> transitions = jdbc.query("""
                SELECT im.stock_before, im.stock_after
                  FROM inventory_movement im JOIN orders o ON o.order_no=im.order_no
                 WHERE o.user_id=? ORDER BY im.stock_before DESC
                """, (rs, row) -> new StockTransition(rs.getInt(1), rs.getInt(2)), fixture.userId());
        assertEquals(10, transitions.size());
        for (int index = 0; index < transitions.size(); index++) {
            StockTransition transition = transitions.get(index);
            assertEquals(10 - index, transition.before());
            assertEquals(9 - index, transition.after());
            assertEquals(1, transition.before() - transition.after());
        }
        record("inventory_competition", json("requestCount", 50, "createdOrders", successful,
                "inventoryInsufficient", insufficient, "finalStock", 0, "orders", 10,
                "orderItems", 10, "inventoryMovements", 10, "movementSequenceContinuous", true));
    }

    @Test
    @Order(2)
    void concurrentSameKeyCreatesOnePhysicalOrderAndReplaysAfterCompletion() throws Exception {
        Fixture fixture = fixture(10, null);
        String idempotencyKey = key("same-key", 0);
        List<Attempt> attempts = concurrently(20, ignored -> orders.submit(
                fixture.userId(), idempotencyKey, request(fixture.skuA(), 1)));

        Set<String> returnedOrderNumbers = new LinkedHashSet<>();
        long inProgress = 0;
        for (Attempt attempt : attempts) {
            if (attempt.successful()) returnedOrderNumbers.add(attempt.order().orderNo());
            if (attempt.hasCode("IDEMPOTENCY_IN_PROGRESS")) inProgress++;
        }
        assertEquals(1, orderCount(fixture.userId()));
        assertEquals(1, itemCount(fixture.userId()));
        assertEquals(1, movementCount(fixture.userId()));
        assertEquals(9, stock(fixture.skuA()));
        assertEquals(1, returnedOrderNumbers.size(), "any completed concurrent callers see the same order number");

        ApiModels.OrderSummary replay = orders.submit(fixture.userId(), idempotencyKey, request(fixture.skuA(), 1));
        assertEquals(returnedOrderNumbers.iterator().next(), replay.orderNo());
        assertEquals(1, orderCount(fixture.userId()));
        assertEquals(1, itemCount(fixture.userId()));
        assertEquals(1, movementCount(fixture.userId()));
        assertEquals(9, stock(fixture.skuA()));
        record("concurrent_idempotency", json("requestCount", 20, "physicalOrders", 1,
                "orderItems", 1, "inventoryMovements", 1, "stockDecrease", 1,
                "distinctOrderNumbers", 1, "inProgressResponses", inProgress,
                "sequentialReplayReturnsOriginal", true));
    }

    @Test
    @Order(3)
    void reusedKeyWithDifferentBodyIsRejectedWithoutAdditionalWrites() {
        Fixture fixture = fixture(10, null);
        String idempotencyKey = key("conflict", 0);
        ApiModels.OrderSummary first = orders.submit(fixture.userId(), idempotencyKey, request(fixture.skuA(), 1));
        int stockAfterFirst = stock(fixture.skuA());
        CommerceException rejected = null;
        try {
            orders.submit(fixture.userId(), idempotencyKey, request(fixture.skuA(), 2));
        } catch (CommerceException exception) {
            rejected = exception;
        }
        assertTrue(rejected != null && "IDEMPOTENCY_KEY_REUSED".equals(rejected.code()));
        assertEquals(1, orderCount(fixture.userId()));
        assertEquals(1, itemCount(fixture.userId()));
        assertEquals(1, movementCount(fixture.userId()));
        assertEquals(stockAfterFirst, stock(fixture.skuA()));
        record("key_reuse_conflict", json("firstOrderCreated", first.orderNo() != null,
                "rejectionCode", "IDEMPOTENCY_KEY_REUSED", "httpStatusEquivalent", 409,
                "orders", 1, "orderItems", 1, "inventoryMovements", 1,
                "stockChangedAfterConflict", false));
    }

    @Test
    @Order(4)
    void mixedSkuRequestRollsBackAllWritesWhenAnySkuIsInsufficient() {
        Fixture fixture = fixture(5, 0);
        int stockBeforeA = stock(fixture.skuA());
        int stockBeforeB = stock(fixture.skuB());
        CommerceException rejected = null;
        try {
            orders.submit(fixture.userId(), key("rollback", 0), new ApiModels.OrderRequest(List.of(
                    new ApiModels.OrderLineRequest(fixture.skuA(), 1),
                    new ApiModels.OrderLineRequest(fixture.skuB(), 1))));
        } catch (CommerceException exception) {
            rejected = exception;
        }
        assertTrue(rejected != null && "INVENTORY_INSUFFICIENT".equals(rejected.code()));
        assertEquals(stockBeforeA, stock(fixture.skuA()));
        assertEquals(stockBeforeB, stock(fixture.skuB()));
        assertEquals(0, orderCount(fixture.userId()));
        assertEquals(0, itemCount(fixture.userId()));
        assertEquals(0, movementCount(fixture.userId()));
        record("transaction_rollback", json("rejectionCode", "INVENTORY_INSUFFICIENT",
                "skuAStockRestored", true, "skuBStockUnchanged", true, "orders", 0,
                "orderItems", 0, "inventoryMovements", 0, "partialWrites", false));
    }

    @Test
    @Order(5)
    void duplicateSkuLinesAreAggregatedBeforeDeductionAndEvidenceWriting() {
        Fixture fixture = fixture(10, null);
        ApiModels.OrderSummary order = orders.submit(fixture.userId(), key("aggregate", 0),
                new ApiModels.OrderRequest(List.of(
                        new ApiModels.OrderLineRequest(fixture.skuA(), 2),
                        new ApiModels.OrderLineRequest(fixture.skuA(), 3))));
        assertEquals(5, order.items().get(0).quantity());
        assertEquals(5, stock(fixture.skuA()));
        assertEquals(1, itemCount(fixture.userId()));
        assertEquals(1, movementCount(fixture.userId()));
        int before = jdbc.queryForObject("SELECT stock_before FROM inventory_movement WHERE order_no=?", Integer.class, order.orderNo());
        int after = jdbc.queryForObject("SELECT stock_after FROM inventory_movement WHERE order_no=?", Integer.class, order.orderNo());
        assertEquals(5, before - after);
        record("duplicate_sku_aggregation", json("requestedLineQuantities", "2+3",
                "aggregatedQuantity", 5, "finalStock", 5, "orderItems", 1,
                "inventoryMovements", 1, "stockBeforeMinusAfter", 5));
    }

    @AfterAll
    static void writeMachineReadableEvidence() throws Exception {
        String destination = System.getenv("ORDER_RELIABILITY_RESULTS_FILE");
        if (destination == null || destination.isBlank()) return;
        StringBuilder json = new StringBuilder("{\n  \"database\": \"MySQL 8.4\",\n  \"scenarios\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : RESULTS.entrySet()) {
            if (index++ > 0) json.append(",\n");
            json.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
        }
        json.append("\n  }\n}\n");
        Files.writeString(Path.of(destination), json.toString(), StandardCharsets.UTF_8);
    }

    private void assertMySql84() {
        String version = jdbc.queryForObject("SELECT VERSION()", String.class);
        assertTrue(version.startsWith("8.4."), "Phase O1 requires the isolated MySQL 8.4 container");
    }

    private Fixture fixture(int stockA, Integer stockB) {
        long base = 8_000_000_000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000_000L);
        long userId = base;
        long categoryId = base + 1;
        long productId = base + 2;
        long skuA = base + 10;
        long skuB = base + 11;
        String suffix = Long.toUnsignedString(base, 36);
        jdbc.update("INSERT INTO user_account(id,username,display_name,status) VALUES (?,?,?,?)", userId, "o1-" + suffix + "@local.invalid", "O1 Test Buyer", "ACTIVE");
        jdbc.update("INSERT INTO product_category(id,name,status,sort_order) VALUES (?,?,?,?)", categoryId, "O1 Reliability", "ACTIVE", 0);
        jdbc.update("INSERT INTO product(id,category_id,product_code,name,description,status,cover_image_path) VALUES (?,?,?,?,?,?,?)", productId, categoryId, "O1-" + suffix, "O1 Reliability Product", "Isolated Phase O1 fixture", "ON_SALE", "/assets/products/product-tshirt-white.png");
        insertSku(productId, skuA, "O1-A-" + suffix, stockA);
        if (stockB != null) insertSku(productId, skuB, "O1-B-" + suffix, stockB);
        return new Fixture(userId, skuA, stockB == null ? null : skuB);
    }

    private void insertSku(long productId, long skuId, String code, int availableStock) {
        jdbc.update("INSERT INTO product_sku(id,product_id,sku_code,color,size,sale_price,currency,status,image_path) VALUES (?,?,?,?,?,?,?,?,?)", skuId, productId, code, "O1", "ONE", new BigDecimal("10.00"), "CNY", "ON_SALE", "/assets/products/product-tshirt-white.png");
        jdbc.update("INSERT INTO inventory(sku_id,available_stock) VALUES (?,?)", skuId, availableStock);
    }

    private List<Attempt> concurrently(int requestCount, IntFunction<ApiModels.OrderSummary> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Attempt>> futures = new ArrayList<>();
            for (int index = 0; index < requestCount; index++) {
                final int requestIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(30, TimeUnit.SECONDS), "concurrent start signal timed out");
                    try {
                        return Attempt.success(action.apply(requestIndex));
                    } catch (CommerceException exception) {
                        return Attempt.commerceFailure(exception);
                    }
                }));
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "all concurrent callers must be ready before release");
            start.countDown();
            List<Attempt> attempts = new ArrayList<>();
            for (Future<Attempt> future : futures) attempts.add(future.get(90, TimeUnit.SECONDS));
            return attempts;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "concurrent executor did not terminate");
        }
    }

    private ApiModels.OrderRequest request(long skuId, int quantity) {
        return new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(skuId, quantity)));
    }

    private String key(String scenario, int sequence) {
        return "o1-" + scenario + "-" + sequence + "-" + UUID.randomUUID();
    }

    private int stock(long skuId) {
        return jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=?", Integer.class, skuId);
    }

    private int orderCount(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id=?", Integer.class, userId);
    }

    private int itemCount(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM order_item oi JOIN orders o ON o.id=oi.order_id WHERE o.user_id=?", Integer.class, userId);
    }

    private int movementCount(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM inventory_movement im JOIN orders o ON o.order_no=im.order_no WHERE o.user_id=?", Integer.class, userId);
    }

    private static synchronized void record(String name, String result) {
        RESULTS.put(name, result);
    }

    private static String json(Object... pairs) {
        StringBuilder value = new StringBuilder("{");
        for (int index = 0; index < pairs.length; index += 2) {
            if (index > 0) value.append(", ");
            value.append('"').append(pairs[index]).append("\": ");
            Object field = pairs[index + 1];
            if (field instanceof String string) value.append('"').append(string.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            else value.append(field);
        }
        return value.append('}').toString();
    }

    private static String value(String key, String fallback) {
        String result = System.getenv(key);
        return result == null || result.isBlank() ? fallback : result;
    }

    private record Fixture(long userId, long skuA, Long skuB) {}
    private record StockTransition(int before, int after) {}
    private record Attempt(ApiModels.OrderSummary order, CommerceException exception) {
        static Attempt success(ApiModels.OrderSummary order) { return new Attempt(order, null); }
        static Attempt commerceFailure(CommerceException exception) { return new Attempt(null, exception); }
        boolean successful() { return order != null; }
        boolean hasCode(String code) { return exception != null && code.equals(exception.code()); }
    }
}
