# P3 Final Acceptance

## Real Screenshot

- Path: `screenshots/v2/03-order-inventory-real.png`
- Viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- Locale: `zh-CN`
- Browser console errors: `0`
- Page errors: `0`

## Runtime Sources

- Vue page: `http://127.0.0.1:5174/`
- Java APIs: `GET /api/orders?userId=1`, `GET /api/orders/{orderNo}`, and `GET /api/orders/{orderNo}/execution-evidence`
- Data source: local MySQL rebuilt from an empty volume, then migrated by Flyway V1-V6.
- Demo seed: `scripts/seed-order-evidence.ps1` calls the real order API.

## Visible Data

- Created order count: `3`
- inventory_movement count: `3`
- Selected screenshot order: `CF1785048273842`
- Selected movement: SKU `10004`, stock `28`, quantity `1`, stock `27`
- Selected idempotency key: `p3-demo-tertiary`
- Initial result: `FIRST_CREATED / 首次创建`

## Request Outcomes

- Same key and same body returned the original order `CF1785048273497` without a new order, deduction, item, or movement.
- Same key with a different body returned HTTP `409`.
- Out-of-stock SKU `10005` returned HTTP `400` and produced no order or movement.
- The order list contains only the three successful creations; replay, 409, and shortage are not fabricated as order rows.

## Integrity and Index Decisions

- Added V6 `UNIQUE(order_no, sku_id, movement_type)` because the original request could contain repeated SKU lines. The service now aggregates same-SKU quantities before deduction, so one order/SKU/type produces one movement.
- No new secondary index was added during final audit. MySQL `EXPLAIN` used `orders.order_no`, the existing OrderItem foreign-key index, and the V6 unique index's `order_no` left prefix. The existing `(user_id, idempotency_key)` unique key serves idempotency lookup; `sku_id` has the foreign-key index.

## Explicit Boundaries

Only `CREATED / 已创建` is supported. Payment, shipment, logistics, refunds, addresses, coupons, production throughput claims, and multi-node in-progress idempotency coordination are not part of P3.
