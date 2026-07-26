package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.catalog.CatalogRepository;
import com.commerceflow.mall.core.CommerceException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final CatalogRepository catalog;
    private final ConcurrentHashMap<String,String> inFlight = new ConcurrentHashMap<>();
    public OrderService(OrderRepository orders, CatalogRepository catalog) { this.orders=orders; this.catalog=catalog; }

    @Transactional
    public ApiModels.OrderSummary submit(long userId, String key, ApiModels.OrderRequest request) {
        if (key == null || key.isBlank()) throw new CommerceException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        String fingerprint=fingerprint(request);
        String lock=userId+":"+key;
        String active=inFlight.putIfAbsent(lock,fingerprint);
        if (active != null) { if (!active.equals(fingerprint)) throw new CommerceException("IDEMPOTENCY_KEY_REUSED", "The same key was used with a different request body"); throw new CommerceException("IDEMPOTENCY_IN_PROGRESS", "The first request is still processing"); }
        try {
            var existing=orders.findByKey(userId,key);
            if (existing.isPresent()) {
                if (!fingerprint.equals(orders.fingerprint(userId,key))) throw new CommerceException("IDEMPOTENCY_KEY_REUSED", "The same key was used with a different request body");
                return existing.get();
            }
            if (request.items().isEmpty()) throw new CommerceException("ORDER_EMPTY", "At least one cart line is required");
            var quantities = new java.util.LinkedHashMap<Long,Integer>();
            for (var line: request.items()) {
                try { quantities.merge(line.skuId(), line.quantity(), Math::addExact); }
                catch (ArithmeticException ex) { throw new CommerceException("ORDER_QUANTITY_INVALID", "Order quantity is too large"); }
            }
            BigDecimal total=BigDecimal.ZERO;
            var resolved=new java.util.ArrayList<ResolvedLine>();
            for (var entry: quantities.entrySet()) {
                long skuId=entry.getKey(); int quantity=entry.getValue();
                ApiModels.Sku sku;
                try { sku=catalog.sku(skuId); } catch (Exception ex) { throw new CommerceException("SKU_NOT_FOUND", "SKU is unavailable"); }
                int stockBefore=orders.stockForUpdate(sku.id());
                if (orders.deduct(sku.id(),quantity)==0) throw new CommerceException("INVENTORY_INSUFFICIENT", "Inventory changed; please retry");
                int stockAfter=orders.currentStock(sku.id());
                total=total.add(sku.salePrice().multiply(BigDecimal.valueOf(quantity))); resolved.add(new ResolvedLine(sku, quantity, stockBefore, stockAfter));
            }
            String no="CF"+System.currentTimeMillis();
            long orderId;
            try { orderId=orders.insertOrder(no,userId,key,fingerprint,total,"CNY"); }
            catch (DuplicateKeyException ex) { throw new CommerceException("IDEMPOTENCY_IN_PROGRESS", "A request with this key is being completed"); }
            for (var resolvedLine: resolved) { var sku=resolvedLine.sku(); orders.insertItem(orderId,sku.id(),sku,catalog.productName(sku.id()),resolvedLine.quantity()); orders.recordInventoryMovement(no,sku.id(),resolvedLine.quantity(),resolvedLine.stockBefore(),resolvedLine.stockAfter(),key); orders.clearCart(userId,sku.id()); }
            return orders.find(no).orElseThrow();
        } finally { inFlight.remove(lock,fingerprint); }
    }
    private record ResolvedLine(ApiModels.Sku sku, int quantity, int stockBefore, int stockAfter) {}
    private String fingerprint(ApiModels.OrderRequest r) { String raw=r.items().stream().map(i->i.skuId()+":"+i.quantity()).sorted().reduce("",(a,b)->a+"|"+b); try { var md=MessageDigest.getInstance("SHA-256"); var out=md.digest(raw.getBytes(StandardCharsets.UTF_8)); return java.util.HexFormat.of().formatHex(out); } catch(Exception e) { throw new IllegalStateException(e); } }
}
