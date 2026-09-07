package com.commerceflow.mall.inventory;

import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.inventory.application.port.out.InventoryPort;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** JDBC adapter for the inventory port; it participates in the caller's transaction. */
@Component
public class JdbcInventoryAdapter implements InventoryPort {
    private final JdbcTemplate jdbc;

    public JdbcInventoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Reservation reserve(long skuId, int quantity) {
        if (quantity < 1) {
            throw new CommerceException("INVENTORY_QUANTITY_INVALID", "Inventory reservation quantity must be positive");
        }
        int before;
        try {
            before = jdbc.queryForObject(
                    "SELECT available_stock FROM inventory WHERE sku_id=? FOR UPDATE",
                    Integer.class,
                    skuId);
        } catch (EmptyResultDataAccessException ex) {
            throw new CommerceException("INVENTORY_NOT_FOUND", "Inventory is unavailable");
        }
        int updated = jdbc.update(
                "UPDATE inventory SET available_stock=available_stock-?,updated_at=CURRENT_TIMESTAMP WHERE sku_id=? AND available_stock>=?",
                quantity,
                skuId,
                quantity);
        if (updated == 0) {
            throw new CommerceException("INVENTORY_INSUFFICIENT", "Inventory changed; please retry");
        }
        int after = jdbc.queryForObject("SELECT available_stock FROM inventory WHERE sku_id=?", Integer.class, skuId);
        return new Reservation(skuId, quantity, before, after);
    }
}
