package com.commerceflow.mall.inventory.application.port.out;

/**
 * Transactional inventory reservation boundary used by order creation.
 * Implementations must reserve one SKU atomically and return the evidence
 * needed by the order execution record.
 */
public interface InventoryPort {
    Reservation reserve(long skuId, int quantity);

    record Reservation(long skuId, int quantity, int stockBefore, int stockAfter) {
    }
}
