# Database Notes

Flyway migrations live in `apps/mall-api/src/main/resources/db/migration`.

- `V1__commerceflow_schema.sql`: tables, unique constraints, foreign keys, and non-negative inventory constraint.
- `V2__demo_data.sql`: one demo user, two products, three SKUs, and stock.

Inventory intentionally has no `locked_stock` or `version` in V1. The single strategy is a guarded atomic update. `orders` stores `idempotency_key` and a request fingerprint, while `order_item` stores product, SKU, attributes, and price snapshots.
