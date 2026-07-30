# Database invariants

- `inventory.available_stock` never becomes negative.
- A successful order writes one order, its aggregated order items, and matching `inventory_movement` rows in the same transaction.
- `inventory_movement` uses the existing unique `(order_no, sku_id, movement_type)` constraint.
- Any insufficient SKU rolls back prior deductions and all order-side writes.
- Scenario-level actual counts are in `results.json`.
