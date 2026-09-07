package com.commerceflow.mall.cart;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.core.CommerceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CartRepository {
    private final JdbcTemplate jdbc;
    public CartRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<ApiModels.CartItem> find(long userId) {
        return jdbc.query("SELECT c.id,c.sku_id,p.name,s.sku_code,s.color,s.size,s.sale_price,s.currency,s.image_path,c.quantity,i.available_stock FROM cart_item c JOIN product_sku s ON s.id=c.sku_id JOIN product p ON p.id=s.product_id JOIN inventory i ON i.sku_id=s.id WHERE c.user_id=? AND p.status='ON_SALE' AND s.status='ON_SALE' ORDER BY c.id", (rs,n) -> new ApiModels.CartItem(rs.getLong("id"), rs.getLong("sku_id"), rs.getString("name"), rs.getString("sku_code"), rs.getString("color"), rs.getString("size"), rs.getBigDecimal("sale_price"), rs.getString("currency"), rs.getString("image_path"), rs.getInt("quantity"), rs.getInt("available_stock")), userId);
    }
    public void add(long userId, long skuId, int quantity) {
        if (!userExists(userId)) throw new CommerceException("USER_NOT_FOUND", "User does not exist");
        if (quantity < 1) throw new CommerceException("CART_QUANTITY_INVALID", "Cart quantity must be greater than zero");
        if (!sellableSkuExists(skuId)) throw new CommerceException("SKU_NOT_FOUND", "SKU is unavailable");
        jdbc.update("INSERT INTO cart_item(user_id,sku_id,quantity) VALUES (?,?,?) ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity)", userId, skuId, quantity);
    }
    public int update(long userId, long itemId, int quantity) {
        return jdbc.update("UPDATE cart_item SET quantity=? WHERE id=? AND user_id=?", quantity, itemId, userId);
    }
    public int delete(long userId, long itemId) {
        return jdbc.update("DELETE FROM cart_item WHERE id=? AND user_id=?", itemId, userId);
    }

    private boolean userExists(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE id=? AND status='ACTIVE'", Integer.class, userId) > 0;
    }

    private boolean sellableSkuExists(long skuId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM product_sku s JOIN product p ON p.id=s.product_id JOIN inventory i ON i.sku_id=s.id WHERE s.id=? AND p.status='ON_SALE' AND s.status='ON_SALE'", Integer.class, skuId) > 0;
    }
}
