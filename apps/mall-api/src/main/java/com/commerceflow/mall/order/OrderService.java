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
            BigDecimal total=BigDecimal.ZERO;
            var resolved=new java.util.ArrayList<ApiModels.Sku>();
            for (var line: request.items()) {
                ApiModels.Sku sku;
                try { sku=catalog.sku(line.skuId()); } catch (Exception ex) { throw new CommerceException("SKU_NOT_FOUND", "SKU is unavailable"); }
                if (sku.availableStock()<line.quantity()) throw new CommerceException("INVENTORY_INSUFFICIENT", "Inventory is insufficient");
                if (orders.deduct(sku.id(),line.quantity())==0) throw new CommerceException("INVENTORY_INSUFFICIENT", "Inventory changed; please retry");
                total=total.add(sku.salePrice().multiply(BigDecimal.valueOf(line.quantity()))); resolved.add(sku);
            }
            String no="CF"+System.currentTimeMillis();
            long orderId;
            try { orderId=orders.insertOrder(no,userId,key,fingerprint,total,"CNY"); }
            catch (DuplicateKeyException ex) { throw new CommerceException("IDEMPOTENCY_IN_PROGRESS", "A request with this key is being completed"); }
            for (int i=0;i<request.items().size();i++) { var line=request.items().get(i); var sku=resolved.get(i); orders.insertItem(orderId,sku.id(),sku,catalog.productName(sku.id()),line.quantity()); orders.logInventory(sku.id(),line.quantity(),no); orders.clearCart(userId,sku.id()); }
            return orders.find(no).orElseThrow();
        } finally { inFlight.remove(lock,fingerprint); }
    }
    private String fingerprint(ApiModels.OrderRequest r) { String raw=r.items().stream().map(i->i.skuId()+":"+i.quantity()).sorted().reduce("",(a,b)->a+"|"+b); try { var md=MessageDigest.getInstance("SHA-256"); var out=md.digest(raw.getBytes(StandardCharsets.UTF_8)); return java.util.HexFormat.of().formatHex(out); } catch(Exception e) { throw new IllegalStateException(e); } }
}
