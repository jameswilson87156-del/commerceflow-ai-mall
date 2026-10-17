package com.commerceflow.mall.order.timeout;

import com.commerceflow.mall.inventory.application.port.out.InventoryPort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.DueOrder;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.StoredItem;
import com.commerceflow.mall.outbox.application.port.out.OutboxEventPort;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels CREATED orders whose cancel deadline has passed and restores the
 * reserved stock in the same transaction.
 *
 * <p>Design choice: the deadline scan reads the database instead of relying on
 * an in-memory or broker delay queue. A scan is naturally idempotent (the
 * status CAS in {@code cancelIfCreated} decides the winner), survives restarts
 * without losing pending cancellations, and keeps the showcase free of a new
 * infrastructure dependency. Timeliness only has to be "good enough" for a
 * 30 minute order window, which a short fixed-delay scan easily meets.</p>
 */
@Service
public class OrderTimeoutCancellationService {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutCancellationService.class);

    private final OrderWritePort orders;
    private final InventoryPort inventory;
    private final OutboxEventPort outbox;
    private final int batchSize;

    public OrderTimeoutCancellationService(
            @Qualifier("orderRepository") OrderWritePort orders,
            InventoryPort inventory,
            OutboxEventPort outbox,
            @Value("${commerceflow.order-timeout.batch-size:50}") int batchSize) {
        this.orders = orders;
        this.inventory = inventory;
        this.outbox = outbox;
        this.batchSize = batchSize;
    }

    /**
     * Enforces deadlines for up to {@code limit} due orders in one transaction.
     * A failure rolls the batch back and the next scan retries; the status CAS
     * guarantees a retried order is never cancelled or restocked twice.
     */
    @Transactional
    public int enforceDeadline(Instant now, int limit) {
        List<DueOrder> due = orders.findTimeoutDue(now, limit);
        int cancelled = 0;
        for (DueOrder order : due) {
            if (cancelOne(order)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    private boolean cancelOne(DueOrder order) {
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

    @Scheduled(fixedDelayString = "${commerceflow.order-timeout.scan-interval-ms:5000}")
    public void scan() {
        try {
            enforceDeadline(Instant.now(), batchSize);
        } catch (Exception ex) {
            // Keep the scheduler alive; the next scan retries the batch.
            log.warn("order timeout scan failed, will retry: {}", ex.getMessage());
        }
    }
}
