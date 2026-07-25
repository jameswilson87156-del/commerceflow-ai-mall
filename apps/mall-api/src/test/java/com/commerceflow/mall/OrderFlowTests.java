package com.commerceflow.mall;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.order.OrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderFlowTests {
    @Autowired OrderService service;
    @Autowired JdbcTemplate jdbc;

    @Test
    void successfulOrderDeductsStockAndCanBeReplayed() {
        int before = jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=10002", Integer.class);
        var request = new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(10002L, 1)));
        var first = service.submit(1, "test-success-10002", request);
        var replay = service.submit(1, "test-success-10002", request);
        assertEquals(first.orderNo(), replay.orderNo());
        assertEquals(before - 1, jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=10002", Integer.class));
    }

    @Test
    void insufficientInventoryRollsBackTheTransaction() {
        int before = jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=10003", Integer.class);
        var request = new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(10003L, before + 1)));
        assertThrows(CommerceException.class, () -> service.submit(1, "test-shortage-10003", request));
        assertEquals(before, jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=10003", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE idempotency_key='test-shortage-10003'", Integer.class));
    }

    @Test
    void sameKeyWithDifferentBodyIsRejected() {
        var first = new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(10001L, 1)));
        service.submit(1, "test-key-reuse", first);
        var different = new ApiModels.OrderRequest(List.of(new ApiModels.OrderLineRequest(10001L, 2)));
        var ex = assertThrows(CommerceException.class, () -> service.submit(1, "test-key-reuse", different));
        assertEquals("IDEMPOTENCY_KEY_REUSED", ex.code());
    }
}
