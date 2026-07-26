# P3 Current Baseline

## Existing Implementation

- `orders` stores `order_no`, `user_id`, `idempotency_key`, request fingerprint, `BigDecimal` total, currency, status, and creation time. Its unique constraint is `UNIQUE (user_id, idempotency_key)`.
- `order_item` stores Product, SKU, product-name, SKU-code, color/size attribute, unit-price, and quantity snapshots.
- `inventory` stores one `available_stock` value per SKU with a non-negative database constraint.
- `OrderService.submit` is annotated with `@Transactional`. It resolves each SKU, performs `UPDATE inventory SET available_stock = available_stock - ? WHERE sku_id = ? AND available_stock >= ?`, inserts the order and items, clears the matching cart item, and returns the created order.
- The request fingerprint and MySQL unique key support replay of the same body and rejection of a reused key with a different body. The existing service tests cover basic success, shortage, replay, and conflict behavior.
- `GET /api/orders` and `GET /api/orders/{orderNo}` return basic orders and item snapshots. There is no Vue order-management page yet.

## Existing Gaps

- The legacy `inventory_change_log` records SKU, quantity, type, and order number only. It cannot prove stock before/after, does not store the idempotency key, and is not suitable for the requested execution-evidence screen.
- There is no dedicated inventory-movement audit record, evidence endpoint, or multi-table read DTO.
- The current API cannot distinguish a persisted initial creation from a replay in an order evidence response; it only returns the order summary.
- The project has no MyBatis dependency or mapper. Existing write behavior is JDBC-based and must remain so.

## What the Current Database Can Prove

- A successful order row, its item snapshots, and the current inventory value.
- The original idempotency key and request fingerprint associated with a successful order.
- A legacy quantity-only change-log row after successful order creation.

## What It Cannot Prove Yet

- The exact stock immediately before and after one successful deduction.
- Whether a movement belongs to a particular idempotency key.
- A page-ready, single-query representation of order, item, inventory, and idempotency evidence.
- That a shortage, conflict, or replay produced no inventory-movement record without an explicit audit query/test.

## Minimum P3 Addition

1. Add a forward-only Flyway migration for `inventory_movement` with exact before/after stock facts and the idempotency key.
2. Preserve the existing JDBC write transaction and record the movement inside it only after a successful conditional deduction.
3. Add MyBatis only for a complex, read-only order-execution DTO and expose it through `GET /api/orders/{orderNo}/execution-evidence`.
4. Add a real Vue order evidence view plus tests, a local API demo script, and reproducible runtime evidence.
