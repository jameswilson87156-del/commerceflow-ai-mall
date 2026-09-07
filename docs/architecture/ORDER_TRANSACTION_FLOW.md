# Order Transaction Flow

```mermaid
sequenceDiagram
  participant C as Client
  participant J as Java OrderService
  participant M as MySQL
  C->>J: POST order + Idempotency-Key
  J->>M: find idempotency record
  alt same key and same request
    M-->>J: original order
    J-->>C: original order
  else new request
    J->>J: validate and aggregate duplicate SKU lines
    J->>M: load SKU / conditional UPDATE available_stock
    alt affected rows = 0
      M-->>J: insufficient inventory
      J-->>C: transaction rollback error
    else stock deducted
      J->>M: insert orders, order_item snapshots, inventory_movement
      J->>M: commit one transaction
      J-->>C: CREATED order
    end
  else same key and different request
    J-->>C: 409 conflict
  end
```

1. Java requires and normalizes `Idempotency-Key`, recursively validates the request, and checks an existing order record.
2. It aggregates duplicate SKU lines before stock work.
3. It sorts distinct SKU IDs before row-lock and stock work. For every SKU it performs `UPDATE inventory ... WHERE available_stock >= quantity`. Zero affected rows means the requested stock is no longer available; no negative stock is written.
4. In the same `@Transactional` JDBC write path it inserts `orders`, `order_item` business snapshots and `inventory_movement` audit rows, then clears matching cart lines.
5. Any stock failure or database exception rolls the transaction back, so no order, movement or partial stock deduction remains.
6. A successful repeat with the same key and fingerprint returns the original order and does not deduct or write evidence again. A reused key with a different request is `409` and writes nothing.

The Showcase money contract is CNY-only. Java rejects a non-CNY SKU before the order is persisted, and Flyway V9 adds database checks for CNY order/product-SKU currency and positive order-item quantities.

After the order claims the durable `(user_id, idempotency_key)` record and completes the order-item, inventory-movement, and cart-cleanup writes, E2 appends one `PENDING` `ORDER_CREATED` row to `outbox_event` in the same transaction. A reservation failure or Outbox insert failure rolls the whole transaction back. E2 has not implemented a broker or delivery worker; the row is evidence of a committed event boundary, not proof of external publication.

MySQL is the fact source for this flow. Redis has no order, inventory, idempotency, or MySQL transaction role. MyBatis is not used for these writes; it reads evidence and overview aggregates only.
