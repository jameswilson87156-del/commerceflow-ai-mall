# P3 Implementation

## Forward-Only Database Change

`V5__add_inventory_movement_evidence.sql` adds nullable color/size snapshot columns to `order_item` and adds `inventory_movement`. The movement records `order_no`, `sku_id`, movement type, quantity, exact stock before/after, idempotency key, and timestamp. Existing migrations were not modified.

The older `inventory_change_log` lacks before/after stock and the key, so it cannot support this evidence screen. P3 leaves the old schema in place but writes the new evidence table for the active order flow.

## Transaction Write Path

`OrderService.submit` remains a JDBC `@Transactional` write path.

1. Resolve the SKU.
2. Lock and read the current inventory row on the server to obtain `stockBefore`.
3. Perform the existing conditional atomic update. `affected rows = 0` means inventory could not satisfy the requested quantity, so the service throws `INVENTORY_INSUFFICIENT`.
4. Read `stockAfter` from the same transaction.
5. Insert `orders`, `order_item` snapshots, and `inventory_movement`, then clear the matching cart item.

The condition `available_stock >= quantity` prevents a successful update from making stock negative. If any later operation fails, Spring rolls back the entire transaction, including the inventory update and movement insert.

## Read APIs

- `GET /api/orders?userId=1`: created orders for the real user filter.
- `GET /api/orders/{orderNo}`: basic order and snapshots.
- `GET /api/orders/{orderNo}/execution-evidence`: MyBatis read DTO containing order facts, snapshots, idempotency key/result, and database-backed inventory movements.

The stored evidence result is `FIRST_CREATED`: it identifies the initial successful order row. It does not claim that a later GET request is a replay event. Replay behavior is proven by the real API script and automated tests.

## Vue Page

`OrderInventoryEvidence.vue` uses the three endpoints above. It has a real `userId` query, local order-number search on the loaded API response, order selection, loading, empty, error, and retry states. The page is read-only and has no payment or fulfillment controls.
