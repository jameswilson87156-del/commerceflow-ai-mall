package com.commerceflow.mall.order.timeout;

import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import com.commerceflow.mall.order.application.port.out.OrderWritePort.DueOrder;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scans for CREATED orders whose cancellation deadline has passed.
 * Each candidate is processed by a separate transaction so one failure cannot
 * roll back other orders from the same scan.
 */
@Service
public class OrderTimeoutCancellationService {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutCancellationService.class);

    private final OrderWritePort orders;
    private final OrderTimeoutCancellationProcessor processor;
    private final int batchSize;

    public OrderTimeoutCancellationService(
            @Qualifier("orderRepository") OrderWritePort orders,
            OrderTimeoutCancellationProcessor processor,
            @Value("${commerceflow.order-timeout.batch-size:50}") int batchSize) {
        this.orders = orders;
        this.processor = processor;
        this.batchSize = batchSize;
    }

    /** Finds a bounded candidate batch, then commits or rolls back each order independently. */
    public int enforceDeadline(Instant now, int limit) {
        List<DueOrder> due = orders.findTimeoutDue(now, limit);
        int cancelled = 0;
        for (DueOrder order : due) {
            try {
                if (processor.cancel(order)) {
                    cancelled++;
                }
            } catch (RuntimeException failure) {
                // The processor transaction has rolled this order back; a later scan can retry it.
                log.warn(
                    "order {} timeout cancellation failed; later orders will continue and this order will retry (errorType={})",
                    order.orderNo(),
                    failure.getClass().getSimpleName()
                );
            }
        }
        return cancelled;
    }

    @Scheduled(fixedDelayString = "${commerceflow.order-timeout.scan-interval-ms:5000}")
    public void scan() {
        try {
            enforceDeadline(Instant.now(), batchSize);
        } catch (Exception failure) {
            // Keep the scheduler alive when candidate lookup itself fails.
            log.warn("order timeout scan failed, will retry (errorType={})", failure.getClass().getSimpleName());
        }
    }
}
