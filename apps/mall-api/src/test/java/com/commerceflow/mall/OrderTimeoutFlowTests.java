package com.commerceflow.mall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.api.CommerceApiContract;
import com.commerceflow.mall.operations.ShowcaseRuntimeProperties;
import com.commerceflow.mall.order.OrderService;
import com.commerceflow.mall.order.timeout.OrderTimeoutCancellationService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the order timeout rule end to end against the shared H2 showcase
 * database: deadline scan, status CAS, stock restore evidence and outbox event.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderTimeoutFlowTests {
    @Autowired OrderService service;
    @Autowired OrderTimeoutCancellationService timeoutService;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    @Autowired ShowcaseRuntimeProperties runtime;

    @Test
    void dueCreatedOrderIsCancelledWithStockRestoredMovementEvidenceAndOutboxEvent() {
        long sku = 10001L;
        int before = stock(sku);
        var order = service.submit(1, key("timeout-main"), request(sku, 2));
        assertEquals("CREATED", order.status());
        assertEquals(before - 2, stock(sku));

        forceDue(order.orderNo());
        timeoutService.enforceDeadline(Instant.now(), 10);

        assertEquals("CANCELLED_TIMEOUT", orderStatus(order.orderNo()));
        assertEquals(before, stock(sku));
        assertEquals(2, count("SELECT COUNT(*) FROM inventory_movement WHERE order_no=?", order.orderNo()));
        assertEquals(1, count(
                "SELECT COUNT(*) FROM inventory_movement WHERE order_no=? AND movement_type='TIMEOUT_RESTOCK'",
                order.orderNo()));
        assertEquals(
                before - 2,
                jdbc.queryForObject(
                        "SELECT stock_before FROM inventory_movement WHERE order_no=? AND movement_type='TIMEOUT_RESTOCK'",
                        Integer.class, order.orderNo()).intValue());
        assertEquals(
                before,
                jdbc.queryForObject(
                        "SELECT stock_after FROM inventory_movement WHERE order_no=? AND movement_type='TIMEOUT_RESTOCK'",
                        Integer.class, order.orderNo()).intValue());
        assertEquals(1, count(
                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_id=? AND event_type='ORDER_TIMEOUT_CANCELLED'",
                order.orderNo()));
    }

    @Test
    void repeatedEnforcementNeverRestocksOrEmitsTwice() {
        long sku = 10002L;
        int before = stock(sku);
        var order = service.submit(1, key("timeout-repeat"), request(sku, 1));
        forceDue(order.orderNo());
        timeoutService.enforceDeadline(Instant.now(), 10);
        timeoutService.enforceDeadline(Instant.now(), 10);

        assertEquals(1, count(
                "SELECT COUNT(*) FROM inventory_movement WHERE order_no=? AND movement_type='TIMEOUT_RESTOCK'",
                order.orderNo()));
        assertEquals(1, count(
                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_id=? AND event_type='ORDER_TIMEOUT_CANCELLED'",
                order.orderNo()));
        assertEquals(before, stock(sku));
    }

    @Test
    void orderThatAlreadyAdvancedPastCreatedKeepsItsStock() {
        long sku = 10001L;
        int before = stock(sku);
        var order = service.submit(1, key("timeout-advanced"), request(sku, 1));
        assertEquals(before - 1, stock(sku));
        jdbc.update("UPDATE orders SET status='PAID' WHERE order_no=?", order.orderNo());
        forceDue(order.orderNo());

        timeoutService.enforceDeadline(Instant.now(), 10);

        assertEquals("PAID", orderStatus(order.orderNo()));
        assertEquals(before - 1, stock(sku));
        assertEquals(0, count(
                "SELECT COUNT(*) FROM inventory_movement WHERE order_no=? AND movement_type='TIMEOUT_RESTOCK'",
                order.orderNo()));
        assertEquals(0, count(
                "SELECT COUNT(*) FROM outbox_event WHERE aggregate_id=? AND event_type='ORDER_TIMEOUT_CANCELLED'",
                order.orderNo()));
    }

    @Test
    void operatorCanTriggerEnforcementThroughTheOperationsApi() throws Exception {
        long sku = 10002L;
        var order = service.submit(1, key("timeout-operator"), request(sku, 1));
        forceDue(order.orderNo());

        String previousMode = null;
        try {
            previousMode = runtime.getOperatorAuthenticationMode();
            runtime.setOperatorAuthenticationMode("DEMO_OPERATOR");
            mockMvc.perform(post(CommerceApiContract.V1_OPERATOR_OPERATIONS_PATH
                            + CommerceApiContract.OPERATIONS_ORDER_TIMEOUT_ENFORCE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cancelled").exists());
        } finally {
            runtime.setOperatorAuthenticationMode(previousMode);
        }

        assertEquals("CANCELLED_TIMEOUT", orderStatus(order.orderNo()));
    }

    @Test
    void newOrdersGetAFutureDeadlineAndAreNotCancelledImmediately() {
        var order = service.submit(1, key("timeout-future"), request(10001L, 1));
        Timestamp deadline = jdbc.queryForObject(
                "SELECT cancel_deadline FROM orders WHERE order_no=?", Timestamp.class, order.orderNo());
        assertNotNull(deadline);
        assertEquals(true, deadline.toInstant().isAfter(Instant.now().minusSeconds(5)));
        assertEquals("CREATED", orderStatus(order.orderNo()));
    }

    private void forceDue(String orderNo) {
        jdbc.update("UPDATE orders SET cancel_deadline=? WHERE order_no=?",
                Timestamp.from(Instant.now().minusSeconds(60)), orderNo);
    }

    private String orderStatus(String orderNo) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE order_no=?", String.class, orderNo);
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

    private String key(String suffix) {
        return "timeout-" + suffix + "-" + UUID.randomUUID();
    }
}
