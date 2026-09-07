package com.commerceflow.mall.order;

import com.commerceflow.mall.api.ApiModels;
import com.commerceflow.mall.catalog.CatalogRepository;
import com.commerceflow.mall.core.CommerceException;
import com.commerceflow.mall.inventory.application.port.out.InventoryPort;
import com.commerceflow.mall.order.application.port.out.IdempotencyStore;
import com.commerceflow.mall.order.application.port.out.OrderWritePort;
import com.commerceflow.mall.outbox.application.port.out.OutboxEventPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderWritePort orders;
    private final IdempotencyStore idempotency;
    private final InventoryPort inventory;
    private final CatalogRepository catalog;
    private final OutboxEventPort outbox;
    private final ConcurrentHashMap<String,String> inFlight = new ConcurrentHashMap<>();
    public OrderService(@Qualifier("orderRepository") OrderWritePort orders,
                        @Qualifier("orderRepository") IdempotencyStore idempotency,
                        InventoryPort inventory,
                        CatalogRepository catalog,
                        OutboxEventPort outbox) {
        this.orders = orders;
        this.idempotency = idempotency;
        this.inventory = inventory;
        this.catalog = catalog;
        this.outbox = outbox;
    }

    @Transactional
    public ApiModels.OrderSummary submit(long userId, String key, ApiModels.OrderRequest request) {
        if (request == null || request.items() == null) throw new CommerceException("ORDER_INVALID", "Order items are required");
        String normalizedKey = normalizeIdempotencyKey(key);
        if (!orders.userExists(userId)) throw new CommerceException("USER_NOT_FOUND", "User does not exist");
        String fingerprint=fingerprint(request);
        String lock=userId+":"+normalizedKey;
        String active=inFlight.putIfAbsent(lock,fingerprint);
        if (active != null) { if (!active.equals(fingerprint)) throw new CommerceException("IDEMPOTENCY_KEY_REUSED", "The same key was used with a different request body"); throw new CommerceException("IDEMPOTENCY_IN_PROGRESS", "The first request is still processing"); }
        try {
            var existing=idempotency.find(userId,normalizedKey);
            if (existing.isPresent()) {
                if (!fingerprint.equals(existing.get().requestFingerprint())) throw new CommerceException("IDEMPOTENCY_KEY_REUSED", "The same key was used with a different request body");
                return existingOrder(existing.get().orderNo());
            }
            if (request.items().isEmpty()) throw new CommerceException("ORDER_EMPTY", "At least one cart line is required");
            var quantities = new TreeMap<Long,Integer>();
            var requestedSkuOrder = new ArrayList<Long>();
            for (var line: request.items()) {
                if (!quantities.containsKey(line.skuId())) requestedSkuOrder.add(line.skuId());
                try { quantities.merge(line.skuId(), line.quantity(), Math::addExact); }
                catch (ArithmeticException ex) { throw new CommerceException("ORDER_QUANTITY_INVALID", "Order quantity is too large"); }
            }
            BigDecimal total=BigDecimal.ZERO;
            String orderCurrency = null;
            var catalogBySku = new HashMap<Long, CatalogLine>();
            for (var entry: quantities.entrySet()) {
                long skuId=entry.getKey(); int quantity=entry.getValue();
                ApiModels.Sku sku;
                try { sku=catalog.sku(skuId); } catch (EmptyResultDataAccessException ex) { throw new CommerceException("SKU_NOT_FOUND", "SKU is unavailable"); }
                if (!ApiModels.SUPPORTED_CURRENCY.equals(sku.currency())) {
                    throw new CommerceException("UNSUPPORTED_CURRENCY", "Only CNY orders are supported");
                }
                if (orderCurrency == null) orderCurrency = sku.currency();
                else if (!orderCurrency.equals(sku.currency())) throw new CommerceException("ORDER_CURRENCY_MISMATCH", "All order lines must use the same currency");
                long productId = catalog.productId(sku.id());
                String productName = catalog.productName(sku.id());
                total = total.add(sku.salePrice().multiply(BigDecimal.valueOf(quantity)));
                catalogBySku.put(skuId, new CatalogLine(
                    productId,
                    sku.id(),
                    productName,
                    sku.skuCode(),
                    sku.color(),
                    sku.size(),
                    sku.salePrice(),
                    sku.currency(),
                    sku.imagePath(),
                    quantity));
            }
            String no=nextOrderNo();
            long orderId;
            try {
                orderId=orders.insertOrder(new OrderWritePort.OrderDraft(
                        no, userId, normalizedKey, fingerprint, total,
                        orderCurrency == null ? ApiModels.SUPPORTED_CURRENCY : orderCurrency));
            } catch (DuplicateKeyException ex) {
                var concurrent = idempotency.find(userId, normalizedKey);
                if (concurrent.isPresent()) {
                    if (!fingerprint.equals(concurrent.get().requestFingerprint())) throw new CommerceException("IDEMPOTENCY_KEY_REUSED", "The same key was used with a different request body");
                    return existingOrder(concurrent.get().orderNo());
                }
                throw new CommerceException("IDEMPOTENCY_IN_PROGRESS", "A request with this key is being completed");
            }
            var resolvedBySku = new HashMap<Long, OrderWritePort.OrderLine>();
            for (var entry : quantities.entrySet()) {
                CatalogLine line = catalogBySku.get(entry.getKey());
                var reservation = inventory.reserve(line.skuId(), line.quantity());
                resolvedBySku.put(line.skuId(), new OrderWritePort.OrderLine(
                        line.productId(), line.skuId(), line.productName(), line.skuCode(), line.color(), line.size(),
                        line.unitPrice(), line.currency(), line.imagePath(), line.quantity(),
                        reservation.stockBefore(), reservation.stockAfter()));
            }
            var resolved = requestedSkuOrder.stream().map(resolvedBySku::get).toList();
            for (var line : resolved) {
                orders.insertItem(orderId, line);
                orders.recordInventoryMovement(no, line.skuId(), line.quantity(), line.stockBefore(), line.stockAfter(), normalizedKey);
                orders.clearCart(userId, line.skuId());
            }
            outbox.append(new OutboxEventPort.OrderCreatedEvent(
                    no,
                    userId,
                    total,
                    orderCurrency == null ? ApiModels.SUPPORTED_CURRENCY : orderCurrency,
                    resolved.stream().map(line -> new OutboxEventPort.OrderCreatedItem(
                            line.skuId(), line.skuCode(), line.productName(), line.unitPrice(), line.quantity())).toList()));
            return orders.find(no).orElseThrow();
        } finally { inFlight.remove(lock,fingerprint); }
    }

    private ApiModels.OrderSummary existingOrder(String orderNo) {
        return orders.find(orderNo).orElseThrow(() -> new CommerceException("IDEMPOTENCY_RESULT_UNAVAILABLE", "The idempotent order result is not available"));
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) throw new CommerceException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        String normalized = key.trim();
        if (normalized.length() > 120) throw new CommerceException("IDEMPOTENCY_KEY_INVALID", "Idempotency-Key must be 120 characters or fewer");
        return normalized;
    }
    private record CatalogLine(
        long productId,
        long skuId,
        String productName,
        String skuCode,
        String color,
        String size,
        BigDecimal unitPrice,
        String currency,
        String imagePath,
        int quantity) {}
    private String fingerprint(ApiModels.OrderRequest r) { String raw=r.items().stream().map(i->i.skuId()+":"+i.quantity()).sorted().reduce("",(a,b)->a+"|"+b); try { var md=MessageDigest.getInstance("SHA-256"); var out=md.digest(raw.getBytes(StandardCharsets.UTF_8)); return java.util.HexFormat.of().formatHex(out); } catch(Exception e) { throw new IllegalStateException(e); } }
    private String nextOrderNo() { return "CF" + java.util.UUID.randomUUID().toString().replace("-", ""); }
}
