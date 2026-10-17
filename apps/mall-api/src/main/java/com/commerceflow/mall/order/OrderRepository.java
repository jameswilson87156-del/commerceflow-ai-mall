package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.order.application.port.out.IdempotencyStore;
import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository implements OrderWritePort, IdempotencyStore {
    private final JdbcTemplate jdbc;
    private final int timeoutSeconds;
    public OrderRepository(JdbcTemplate jdbc,
            @Value("${commerceflow.order-timeout.ttl-seconds:1800}") int timeoutSeconds) {
        this.jdbc = jdbc;
        this.timeoutSeconds = timeoutSeconds;
    }
    @Override
    public Optional<IdempotencyRecord> find(long userId, String key) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT order_no,request_fingerprint FROM orders WHERE user_id=? AND idempotency_key=?",
                    (rs, n) -> new IdempotencyRecord(rs.getString("order_no"), rs.getString("request_fingerprint")),
                    userId,
                    key));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean userExists(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE id=? AND status='ACTIVE'", Integer.class, userId) > 0;
    }

    @Override
    public long insertOrder(OrderDraft draft) {
        // The timeout deadline is computed in the application and stored as a
        // plain parameter so the same statement runs on MySQL and H2 alike.
        Timestamp deadline = Timestamp.from(Instant.now().plusSeconds(timeoutSeconds));
        jdbc.update("INSERT INTO orders(order_no,user_id,idempotency_key,request_fingerprint,total_amount,currency,status,cancel_deadline) VALUES (?,?,?,?,?,?,?,?)",
                draft.orderNo(), draft.userId(), draft.idempotencyKey(), draft.requestFingerprint(), draft.totalAmount(), draft.currency(), "CREATED", deadline);
        return jdbc.queryForObject("SELECT id FROM orders WHERE order_no=?", Long.class, draft.orderNo());
    }

    @Override
    public void insertItem(long orderId, OrderLine line) {
        jdbc.update("INSERT INTO order_item(order_id,product_id,sku_id,product_name_snapshot,sku_code_snapshot,sku_attributes_snapshot,color_snapshot,size_snapshot,image_path_snapshot,unit_price,quantity) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
            orderId,
            line.productId(),
            line.skuId(),
            line.productName(),
            line.skuCode(),
            line.color()+" / "+line.size(),
            line.color(),
            line.size(),
            line.imagePath(),
            line.unitPrice(),
            line.quantity());
    }
    @Override
    public void recordInventoryMovement(String orderNo, long skuId, int quantity, int stockBefore, int stockAfter, String key) {
        jdbc.update("INSERT INTO inventory_movement(order_no,sku_id,movement_type,quantity,stock_before,stock_after,idempotency_key) VALUES (?,?,?,?,?,?,?)", orderNo, skuId, "ORDER_DEDUCT", quantity, stockBefore, stockAfter, key);
    }
    @Override
    public void clearCart(long userId, long skuId) { jdbc.update("DELETE FROM cart_item WHERE user_id=? AND sku_id=?", userId, skuId); }

    @Override
    public List<DueOrder> findTimeoutDue(Instant now, int limit) {
        return jdbc.query(
                "SELECT order_no,user_id FROM orders WHERE status='CREATED' AND cancel_deadline IS NOT NULL AND cancel_deadline<=? ORDER BY cancel_deadline LIMIT ?",
                (rs, n) -> new DueOrder(rs.getString("order_no"), rs.getLong("user_id")),
                Timestamp.from(now), limit);
    }

    @Override
    public boolean cancelIfCreated(String orderNo) {
        return jdbc.update("UPDATE orders SET status='CANCELLED_TIMEOUT' WHERE order_no=? AND status='CREATED'", orderNo) > 0;
    }

    @Override
    public List<StoredItem> storedItems(String orderNo) {
        return jdbc.query(
                "SELECT i.sku_id,i.sku_code_snapshot,i.quantity FROM order_item i WHERE i.order_id=(SELECT id FROM orders WHERE order_no=?) ORDER BY i.id",
                (rs, n) -> new StoredItem(rs.getLong(1), rs.getString(2), rs.getInt(3)),
                orderNo);
    }

    @Override
    public void recordTimeoutRestock(String orderNo, long skuId, int quantity, int stockBefore, int stockAfter, String evidenceKey) {
        jdbc.update("INSERT INTO inventory_movement(order_no,sku_id,movement_type,quantity,stock_before,stock_after,idempotency_key) VALUES (?,?,?,?,?,?,?)",
                orderNo, skuId, "TIMEOUT_RESTOCK", quantity, stockBefore, stockAfter, evidenceKey);
    }
    public List<ApiModels.OrderSummary> findAll(long userId) { return jdbc.query("SELECT order_no,user_id,total_amount,currency,status,created_at FROM orders WHERE user_id=? ORDER BY id DESC", (rs,n) -> summary(rs, rs.getLong("user_id")), userId); }
    /** Cross-user list used only after the operator authorization policy succeeds. */
    public List<ApiModels.OrderSummary> findAllForOperator() {
        return jdbc.query("SELECT order_no,user_id,total_amount,currency,status,created_at FROM orders ORDER BY id DESC",
                (rs, n) -> summary(rs, rs.getLong("user_id")));
    }
    @Override
    public Optional<ApiModels.OrderSummary> find(String no) { try { return Optional.of(jdbc.queryForObject("SELECT order_no,user_id,total_amount,currency,status,created_at FROM orders WHERE order_no=?", (rs,n)->summary(rs,rs.getLong("user_id")), no)); } catch (EmptyResultDataAccessException ex) { return Optional.empty(); } }
    public Optional<ApiModels.OrderSummary> findForUser(long userId, String no) { try { return Optional.of(jdbc.queryForObject("SELECT order_no,user_id,total_amount,currency,status,created_at FROM orders WHERE order_no=? AND user_id=?", (rs,n)->summary(rs,rs.getLong("user_id")), no, userId)); } catch (EmptyResultDataAccessException ex) { return Optional.empty(); } }
    private ApiModels.OrderSummary summary(ResultSet rs, long userId) throws SQLException {
        String no=rs.getString("order_no");
        List<ApiModels.OrderItem> items=jdbc.query("SELECT product_name_snapshot,sku_code_snapshot,color_snapshot,size_snapshot,image_path_snapshot,unit_price,quantity FROM order_item WHERE order_id=(SELECT id FROM orders WHERE order_no=?) ORDER BY id", (r,n)->new ApiModels.OrderItem(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getBigDecimal(6),r.getInt(7)), no);
        return new ApiModels.OrderSummary(no,userId,rs.getBigDecimal("total_amount"),rs.getString("currency"),rs.getString("status"),rs.getTimestamp("created_at").toInstant(),items);
    }
}
