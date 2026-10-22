package com.commerceflow.mall;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.order.OrderService;
import com.commerceflow.mall.order.timeout.OrderTimeoutCancellationService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MySQL proof for per-order rollback, retry, and concurrent timeout scans. */
@SpringBootTest(properties = "commerceflow.order-timeout.scan-interval-ms=3600000")
@EnabledIfEnvironmentVariable(named = "ORDER_RELIABILITY_MYSQL_ENABLED", matches = "true")
class OrderTimeoutCancellationMySqlTests {

    @Autowired OrderService orders;
    @Autowired OrderTimeoutCancellationService timeoutService;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void isolatedMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("ORDER_RELIABILITY_DB_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("ORDER_RELIABILITY_DB_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("ORDER_RELIABILITY_DB_PASSWORD"));
        registry.add("commerceflow.ai.rate-limit.enabled", () -> "false");
    }

    @Test
    void oneOutboxFailureRollsBackOnlyThatOrderAndTheNextScanRetriesIt() {
        long skuId = 10001L;
        int stockBefore = stock(skuId);
        var first = orders.submit(1L, key("batch-first"), request(skuId, 1));
        var second = orders.submit(1L, key("batch-second"), request(skuId, 1));
        assertEquals(stockBefore - 2, stock(skuId));

        Instant now = Instant.now();
        due(first.orderNo(), now.minusSeconds(120));
        due(second.orderNo(), now.minusSeconds(60));
        String conflictingEventId = "order-timeout-cancelled:" + second.orderNo();
        jdbc.update(
                "INSERT INTO outbox_event(event_id,aggregate_type,aggregate_id,event_type,payload_json,status,attempts) VALUES (?,?,?,?,?,?,?)",
                conflictingEventId, "ORDER", second.orderNo(), "TEST_CONFLICT", "{}", "PENDING", 0);

        int cancelled;
        try {
            cancelled = assertDoesNotThrow(
                    () -> timeoutService.enforceDeadline(now, 10),
                    "a failed outbox insert for one order must not abort the rest of the batch");
        } finally {
            jdbc.update("DELETE FROM outbox_event WHERE event_id=?", conflictingEventId);
        }

        assertEquals(1, cancelled);
        assertEquals("CANCELLED_TIMEOUT", status(first.orderNo()));
        assertEquals("CREATED", status(second.orderNo()));
        assertEquals(stockBefore - 1, stock(skuId));
        assertEquals(1, timeoutRestockCount(first.orderNo()));
        assertEquals(0, timeoutRestockCount(second.orderNo()));
        assertEquals(1, timeoutEventCount(first.orderNo()));
        assertEquals(0, timeoutEventCount(second.orderNo()));

        assertEquals(1, timeoutService.enforceDeadline(now, 10));
        assertEquals("CANCELLED_TIMEOUT", status(second.orderNo()));
        assertEquals(stockBefore, stock(skuId));
        assertEquals(1, timeoutRestockCount(first.orderNo()));
        assertEquals(1, timeoutRestockCount(second.orderNo()));
        assertEquals(1, timeoutEventCount(first.orderNo()));
        assertEquals(1, timeoutEventCount(second.orderNo()));
        assertEquals(0, timeoutService.enforceDeadline(now, 10));
        assertEquals(stockBefore, stock(skuId));
    }

    @Test
    void concurrentScansCancelAndRestockAnOrderOnlyOnce() throws Exception {
        long skuId = 10001L;
        int stockBefore = stock(skuId);
        var order = orders.submit(1L, key("concurrent"), request(skuId, 1));
        Instant now = Instant.now();
        due(order.orderNo(), now.minusSeconds(60));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> scanWhenReleased(ready, start, now));
            Future<Integer> second = executor.submit(() -> scanWhenReleased(ready, start, now));
            assertTrue(ready.await(30, TimeUnit.SECONDS), "both scans must be ready before release");
            start.countDown();
            assertEquals(1, first.get(60, TimeUnit.SECONDS) + second.get(60, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "scan executor must terminate");
        }

        assertEquals("CANCELLED_TIMEOUT", status(order.orderNo()));
        assertEquals(stockBefore, stock(skuId));
        assertEquals(1, timeoutRestockCount(order.orderNo()));
        assertEquals(1, timeoutEventCount(order.orderNo()));
        assertEquals(0, timeoutService.enforceDeadline(now, 10));
        assertEquals(1, timeoutRestockCount(order.orderNo()));
    }

    private int scanWhenReleased(CountDownLatch ready, CountDownLatch start, Instant now) throws Exception {
        ready.countDown();
        if (!start.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("concurrent scan start timed out");
        }
        return timeoutService.enforceDeadline(now, 10);
    }

    private void due(String orderNo, Instant deadline) {
        jdbc.update("UPDATE orders SET cancel_deadline=? WHERE order_no=?", Timestamp.from(deadline), orderNo);
    }

    private String status(String orderNo) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE order_no=?", String.class, orderNo);
    }

    private int stock(long skuId) {
        return jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=?", Integer.class, skuId);
    }

    private int timeoutRestockCount(String orderNo) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE order_no=? AND movement_type='TIMEOUT_RESTOCK'",
                Integer.class, orderNo);
    }

    private int timeoutEventCount(String orderNo) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_id=? AND event_type='ORDER_TIMEOUT_CANCELLED'",
                Integer.class, orderNo);
    }

    private ApiModels.OrderRequest request(long skuId, int quantity) {
        return new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(skuId, quantity)));
    }

    private String key(String prefix) {
        return "timeout-mysql-" + prefix + "-" + UUID.randomUUID();
    }
}
