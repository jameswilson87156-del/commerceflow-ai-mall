# Data Model

| Aggregate / record | Key facts |
| --- | --- |
| Product / product_category | Product display data and on-sale status. |
| product_sku | SKU code, color, size, BigDecimal sale price, currency, image path. |
| inventory | One available-stock value per SKU and update timestamp. |
| cart_item | Demo-user cart lines. |
| orders | Unique order number; user/key/fingerprint uniqueness supports idempotency; total, currency, `CREATED`. |
| order_item | Product/SKU/name/attribute/image/price snapshots preserve the order view. |
| inventory_movement | Stored `ORDER_DEDUCT` evidence with before/quantity/after and idempotency context. |
| ai_trace | Java-owned question summary, facts-derived evidence JSON, provider/fallback information, latency, error state. |

Flyway V1-V8 establishes this current local schema. No payment, delivery, address, coupon, user-profile, or analytical fact tables are claimed.
