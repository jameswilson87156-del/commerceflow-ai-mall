package com.commerceflow.mall.outbox.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Transactional append boundary for events emitted by business writes. */
public interface OutboxEventPort {
    void append(OrderCreatedEvent event);

    record OrderCreatedEvent(
            String orderNo,
            long userId,
            BigDecimal totalAmount,
            String currency,
            List<OrderCreatedItem> items) {
    }

    record OrderCreatedItem(
            long skuId,
            String skuCode,
            String productName,
            BigDecimal unitPrice,
            int quantity) {
    }
}
