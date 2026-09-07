package com.commerceflow.mall.order.application.port.out;

import com.commerceflow.mall.api.ApiModels;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * The order use case's write boundary.
 *
 * <p>The order application layer deliberately does not depend on the JDBC
 * repository type. The current adapter still uses the existing schema and
 * transaction, but a later persistence implementation can replace it without
 * changing order orchestration.</p>
 */
@NoRepositoryBean
public interface OrderWritePort {
    boolean userExists(long userId);

    long insertOrder(OrderDraft draft);

    void insertItem(long orderId, OrderLine line);

    void recordInventoryMovement(String orderNo, long skuId, int quantity, int stockBefore, int stockAfter, String idempotencyKey);

    void clearCart(long userId, long skuId);

    Optional<ApiModels.OrderSummary> find(String orderNo);

    record OrderDraft(
            String orderNo,
            long userId,
            String idempotencyKey,
            String requestFingerprint,
            BigDecimal totalAmount,
            String currency) {
    }

    record OrderLine(
            long productId,
            long skuId,
            String productName,
            String skuCode,
            String color,
            String size,
            BigDecimal unitPrice,
            String currency,
            String imagePath,
            int quantity,
            int stockBefore,
            int stockAfter) {
    }
}
