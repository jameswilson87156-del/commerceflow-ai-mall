package com.commerceflow.mall.order.timeout;

import com.commerceflow.mall.inventory.application.port.out.InventoryPort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.DueOrder;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.StoredItem;
import com.commerceflow.mall.outbox.application.port.out.OutboxEventPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Executes one timeout cancellation, including its stock evidence and outbox event, atomically. */
@Service
public class OrderTimeoutCancellationProcessor {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutCancellationProcessor.class);

    private final OrderWritePort orders;
    private final InventoryPort inventory;
    private final OutboxEventPort outbox;

    public OrderTimeoutCancellationProcessor(
            @Qualifier("orderRepository") OrderWritePort orders,
            InventoryPort inventory,
            OutboxEventPort outbox) {
        this.orders = orders;
        this.inventory = inventory;
        this.outbox = outbox;
    }

    /**
     * Runs in an independent transaction so a failed order can roll back without
     * undoing successful cancellations earlier in the same scan.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean cancel(DueOrder order) {
        if (!orders.cancelIfCreated(order.orderNo())) {
            // The order advanced concurrently (paid or already cancelled);
            // its stock must stay exactly as the winning transition left it.
            return false;
        }
        List<StoredItem> items = orders.storedItems(order.orderNo());
        for (StoredItem item : items) {
            InventoryPort.Reservation restock = inventory.release(item.skuId(), item.quantity());
            orders.recordTimeoutRestock(
                    order.orderNo(),
                    item.skuId(),
                    item.quantity(),
                    restock.stockBefore(),
                    restock.stockAfter(),
                    "TIMEOUT:" + order.orderNo());
        }
        outbox.append(new OutboxEventPort.OrderTimeoutCancelledEvent(
                order.orderNo(),
                order.userId(),
                items.stream()
                        .map(item -> new OutboxEventPort.CancelledItem(item.skuId(), item.skuCode(), item.quantity()))
                        .toList()));
        log.info("order {} cancelled by timeout, {} sku(s) restocked", order.orderNo(), items.size());
        return true;
    }
}

