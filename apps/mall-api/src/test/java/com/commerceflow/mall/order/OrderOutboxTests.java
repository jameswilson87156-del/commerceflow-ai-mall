package com.commerceflow.mall.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class OrderOutboxTests {
    @Autowired OrderService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void successfulOrderAndPendingOrderCreatedEventAreStoredTogether() {
        var order = service.submit(1, key("append"), request(10004, 1));

        assertEquals(1, count("SELECT COUNT(*) FROM outbox_event WHERE aggregate_id=?", order.orderNo()));
        assertEquals("PENDING", jdbc.queryForObject(
                "SELECT status FROM outbox_event WHERE aggregate_id=?",
                String.class,
                order.orderNo()));
        String payload = jdbc.queryForObject(
                "SELECT payload_json FROM outbox_event WHERE aggregate_id=?",
                String.class,
                order.orderNo());
        assertTrue(payload.contains("\"orderNo\":\"" + order.orderNo() + "\""));
        assertTrue(payload.contains("\"currency\":\"CNY\""));
        assertTrue(payload.contains("\"skuId\":10004"));
    }

    @Test
    void sameIdempotencyReplayDoesNotAppendAnotherOutboxEvent() {
        String key = key("replay");
        var first = service.submit(1, key, request(10004, 1));
        var replay = service.submit(1, key, request(10004, 1));

        assertEquals(first.orderNo(), replay.orderNo());
        assertEquals(1, count("SELECT COUNT(*) FROM outbox_event WHERE aggregate_id=?", first.orderNo()));
    }

    @Test
    void failedInventoryReservationRollsBackOrderAndOutboxWrite() {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event", Integer.class);

        assertThrows(CommerceException.class, () -> service.submit(1, key("rollback"), request(10005, 1)));

        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event", Integer.class));
    }

    private ApiModels.OrderRequest request(long skuId, int quantity) {
        return new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(skuId, quantity)));
    }

    private int count(String sql, Object value) {
        return jdbc.queryForObject(sql, Integer.class, value);
    }

    private String key(String suffix) {
        return "e2-" + suffix + "-" + UUID.randomUUID();
    }
}
