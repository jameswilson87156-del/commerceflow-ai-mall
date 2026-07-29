# Data Model

The list below records the current core tables and relationships without duplicating the full Flyway DDL.

| Table | Role and key facts |
| --- | --- |
| `product` | Product name, description, status and category relationship. |
| `product_sku` | SKU code is unique; stores color, size, sale price, currency, status and current image path. |
| `inventory` | One row per SKU with non-negative `available_stock`. |
| `cart_item` | Demo user and SKU are unique together; quantity is positive. |
| `orders` | Unique `order_no`; unique `(user_id, idempotency_key)`; money is `DECIMAL(19,2)` and status is currently only `CREATED`. |
| `order_item` | Product/SKU/name/attributes/price/quantity snapshots; `image_path_snapshot` preserves the order-time image reference. |
| `inventory_movement` | Stored `ORDER_DEDUCT` audit record with before, quantity, after, order and idempotency reference. |
| `ai_trace` | Bounded request category summary, provider/result metadata and trace-related fields; it does not store API keys, raw prompts or exception stacks. |

`order_item` is an immutable business snapshot, not a live join to a product. `inventory_movement` is evidence of a successful transaction. The MySQL uniqueness constraints support idempotency; Redis does not participate in these records.
