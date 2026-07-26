# CommerceFlow Schema Naming Audit

**Audited source:** Flyway `V1__commerceflow_schema.sql` through `V7__add_order_item_image_snapshot.sql`
**Status:** P3.1 naming decision implemented; no rename programme was introduced.

## Summary

CommerceFlow's short, module-neutral names suit a modular monolith. `product`, `product_sku`, `orders`, `order_item`, `inventory`, and `inventory_movement` are clearer than adding `mall_`, `tb_`, or `sys_` prefixes. The primary P3.1 addition should be an explicit `image_path_snapshot` on `order_item`, not a general rename programme.

| Audit item | Status | Finding and decision |
| --- | --- | --- |
| `product` and `product_sku` | KEEP | The parent/variant distinction is clear and matches the current API. |
| `orders` | KEEP | Avoids the reserved word `order`; plural naming is deliberate and already consistently referenced. |
| `order_item` | KEEP | Singular child table matches `cart_item` and is clear. |
| `product_category` | KEEP | Explicit and clearer than an ambiguous `category` table in a future multi-domain application. |
| `inventory` | KEEP | One row per SKU, enforced by `UNIQUE(sku_id)`. |
| `inventory_movement` | KEEP | Names the evidence/audit role directly. V6 additionally enforces one `(order_no, sku_id, movement_type)` record. |
| `inventory_change_log` | NEEDS_DECISION | V1 legacy table is not written or queried by P3; P3 uses `inventory_movement`. Do not remove it now. Assess removal only in a later migration after deployment/history policy is agreed. |
| `status` on product, SKU, orders, and user | KEEP | Context is supplied by the containing table. `order_status` would duplicate context without a practical gain. |
| `order_no` | KEEP | Conventional, human-readable business identifier; unique and referenced by movement evidence. |
| `idempotency_key` | KEEP | Matches the HTTP `Idempotency-Key` concept and has `UNIQUE(user_id, idempotency_key)`. |
| `request_fingerprint` | KEEP | Explicitly distinguishes request-body conflict detection from the idempotency key. |
| `quantity`, `stock_before`, `stock_after` | KEEP | Direct, unambiguous quantities. `quantity` is positive by constraint in movement records. |
| `created_at`, `updated_at` | KEEP / ADD LATER | Existing names are consistent. Only `inventory` currently needs `updated_at`; add it to other mutable tables only when a concrete audit/update use case exists. |
| `cover_image_path` on product | KEEP | Clearly denotes the product-level default image. |
| `image_path` on product_sku | KEEP | Concise because the containing table establishes SKU scope. |
| `image_path_snapshot` on order_item | KEEP | V7 field. It clearly states a historical order-item asset path and does not overload live SKU `image_path`. |
| `product_code`, `sku_code` | KEEP | Product and SKU identifiers have distinct business roles; both remain useful in a showcase. |
| Numeric types | KEEP | `BIGINT` ids, `DECIMAL(19,2)` monetary fields, and present `VARCHAR` sizes are sufficient for the local showcase. |

## Current Structural Inventory

| Table | Primary key | Relationships / constraints | Important indexes |
| --- | --- | --- | --- |
| `user_account` | `id` | `username` unique | primary, `username` unique |
| `product_category` | `id` | none | primary |
| `product` | `id` | `category_id -> product_category.id` | primary, category FK support |
| `product_sku` | `id` | `product_id -> product.id`; `sku_code` unique | primary, `sku_code` unique |
| `inventory` | `id` | `sku_id -> product_sku.id`; one row per SKU | primary, `sku_id` unique |
| `cart_item` | `id` | user and SKU FKs; one user/SKU row | primary, `UNIQUE(user_id, sku_id)` |
| `orders` | `id` | user FK; unique business number and idempotency pair | primary, `order_no` unique, `UNIQUE(user_id, idempotency_key)` |
| `order_item` | `id` | `order_id -> orders.id` | primary |
| `inventory_change_log` | `id` | no declared FK | primary |
| `inventory_movement` | `id` | `order_no -> orders.order_no`; `sku_id -> product_sku.id` | primary, `idx_inventory_movement_order_no`, `UNIQUE(order_no, sku_id, movement_type)` |
| `ai_trace` | `id` | `trace_id` unique | primary, `trace_id` unique |

## P3.1 Naming Boundary

V7 adds only `order_item.image_path_snapshot VARCHAR(255)`. It stores the local asset path selected at order creation. No current table needs a prefix, no current status needs renaming, and no external schema was imported.
