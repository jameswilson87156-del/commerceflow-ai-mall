package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;
    public OrderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<ApiModels.OrderSummary> findByKey(long userId, String key) {
        try { String no = jdbc.queryForObject("SELECT order_no FROM orders WHERE user_id=? AND idempotency_key=?", String.class, userId, key); return find(no); }
        catch (EmptyResultDataAccessException ex) { return Optional.empty(); }
    }
    public String fingerprint(long userId, String key) {
        try { return jdbc.queryForObject("SELECT request_fingerprint FROM orders WHERE user_id=? AND idempotency_key=?", String.class, userId, key); }
        catch (EmptyResultDataAccessException ex) { return null; }
    }
    public long insertOrder(String orderNo, long userId, String key, String fingerprint, BigDecimal total, String currency) {
        jdbc.update("INSERT INTO orders(order_no,user_id,idempotency_key,request_fingerprint,total_amount,currency,status) VALUES (?,?,?,?,?,?,?)", orderNo,userId,key,fingerprint,total,currency,"CREATED");
        return jdbc.queryForObject("SELECT id FROM orders WHERE order_no=?", Long.class, orderNo);
    }
    public void insertItem(long orderId, long productId, ApiModels.Sku sku, String productName, int quantity) {
        jdbc.update("INSERT INTO order_item(order_id,product_id,sku_id,product_name_snapshot,sku_code_snapshot,sku_attributes_snapshot,unit_price,quantity) VALUES (?,?,?,?,?,?,?,?)", orderId, productId, sku.id(), productName, sku.skuCode(), sku.color()+" / "+sku.size(), sku.salePrice(), quantity);
    }
    public void logInventory(long skuId, int quantity, String orderNo) { jdbc.update("INSERT INTO inventory_change_log(sku_id,change_type,quantity,order_no) VALUES (?,?,?,?)", skuId,"ORDER_DEDUCT",quantity,orderNo); }
    public void clearCart(long userId, long skuId) { jdbc.update("DELETE FROM cart_item WHERE user_id=? AND sku_id=?", userId, skuId); }
    public int deduct(long skuId, int quantity) { return jdbc.update("UPDATE inventory SET available_stock=available_stock-?,updated_at=CURRENT_TIMESTAMP WHERE sku_id=? AND available_stock>=?", quantity, skuId, quantity); }
    public List<ApiModels.OrderSummary> findAll(long userId) { return jdbc.query("SELECT order_no,user_id,total_amount,currency,status,created_at FROM orders WHERE user_id=? ORDER BY id DESC", (rs,n) -> summary(rs, rs.getLong("user_id")), userId); }
    public Optional<ApiModels.OrderSummary> find(String no) { try { return Optional.of(jdbc.queryForObject("SELECT order_no,user_id,total_amount,currency,status,created_at FROM orders WHERE order_no=?", (rs,n)->summary(rs,rs.getLong("user_id")), no)); } catch (EmptyResultDataAccessException ex) { return Optional.empty(); } }
    private ApiModels.OrderSummary summary(ResultSet rs, long userId) throws SQLException {
        String no=rs.getString("order_no");
        List<ApiModels.OrderItem> items=jdbc.query("SELECT product_name_snapshot,sku_code_snapshot,sku_attributes_snapshot,unit_price,quantity FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?)", (r,n)->new ApiModels.OrderItem(r.getString(1),r.getString(2),r.getString(3),r.getBigDecimal(4),r.getInt(5)), no);
        return new ApiModels.OrderSummary(no,rs.getBigDecimal("total_amount"),rs.getString("currency"),rs.getString("status"),rs.getTimestamp("created_at").toInstant(),items);
    }
}
