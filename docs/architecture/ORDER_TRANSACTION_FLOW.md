# Order Transaction Flow

```mermaid
sequenceDiagram
  participant C as Client
  participant J as OrderService
  participant M as MySQL
  C->>J: items + Idempotency-Key
  J->>M: find user/key and fingerprint
  alt existing same request
    M-->>J: original order
    J-->>C: original order, no new movement
  else new request
    J->>M: conditional stock UPDATE per aggregated SKU
    J->>M: insert orders and order_item snapshots
    J->>M: insert inventory_movement evidence
    J->>M: clear matching cart items
    J-->>C: CREATED order number
  else reused key, different request
    J-->>C: 409 conflict
  end
```

The conditional `UPDATE inventory ... WHERE available_stock >= quantity` returns zero affected rows for insufficient/concurrently changed stock, preventing a negative value. The service is transactional: a failed deduction, insert, or later operation rolls back order, item, movement, and cart mutation. The stored uniqueness constraint protects order/SKU/`ORDER_DEDUCT` movement evidence; idempotent replays do not create duplicate effects.
