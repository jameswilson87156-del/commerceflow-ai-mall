# P3 Interview Notes

## Why OrderItem Snapshots?

Product names, SKU codes, colors, sizes, and prices can change later. `OrderItem` keeps the facts used when the order was created, so historical order display does not depend on the current catalog.

## Why Conditional Atomic UPDATE?

`UPDATE inventory ... WHERE available_stock >= quantity` succeeds only when enough stock remains. An affected-row count of zero means no safe deduction happened, preventing negative stock without relying on a frontend pre-check.

## Why a Transaction?

Inventory deduction, order creation, item snapshots, movement evidence, and cart cleanup describe one business action. The transaction makes a later failure roll back earlier writes, so there is no orphan movement or silently reduced stock.

## Why MySQL Unique Idempotency?

`UNIQUE (user_id, idempotency_key)` makes the successful order result durable across process restarts. The request fingerprint distinguishes a valid replay from reuse of the same key with a different body.

## Why Must Replay Not Deduct Again?

A network retry represents the original user intent, not a second purchase. Returning the stored order prevents duplicate inventory deduction, OrderItem insertion, and movement creation.

## Why MyBatis Only for the Read Model?

The existing JDBC transaction is small and already tested. MyBatis is useful here for one nested multi-table display DTO; using it only for the read model avoids a technology-driven rewrite of the proven write path.

## What Does inventory_movement Solve?

It records the exact SKU, quantity, before/after stock, order number, idempotency key, and time for a successful deduction. That makes the evidence page a database-backed explanation rather than a frontend inference from current inventory.
