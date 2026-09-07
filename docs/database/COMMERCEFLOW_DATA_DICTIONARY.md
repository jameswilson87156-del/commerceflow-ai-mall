# CommerceFlow Data Dictionary

**Source of truth for this document:** Flyway migrations V1-V10 on the current implementation branch.
**Scope:** Current local Showcase schema. V7 adds the implemented `order_item.image_path_snapshot` field, V9 enforces the CNY-only money contract and positive order-item quantities, and V10 adds the pending transactional Outbox foundation.

## `user_account` - 用户演示账户

Responsibility: local demo-login identity. Primary key: `id BIGINT`. Indexes: primary key and unique `username`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Internal user id |
| `username` | VARCHAR(80), no | Unique login name |
| `display_name` | VARCHAR(120), no | Display label |
| `status` | VARCHAR(20), no | Account state |
| `created_at` | TIMESTAMP, no, current timestamp | Creation time |

Boundary: demo identity only; no profile or private-data model.

## `product_category` - 商品分类

Responsibility: product grouping. Primary key: `id BIGINT`. Source: CommerceFlow原创; relationship idea independently compared with mall/litemall.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Category id |
| `name` | VARCHAR(120), no | Display category name |
| `status` | VARCHAR(20), no | Category state |
| `sort_order` | INT, no, 0 | Display sort order |

## `product` - 商品（SPU）

Responsibility: product-level display and default image. Primary key: `id BIGINT`; foreign key: `category_id -> product_category.id`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Product id |
| `category_id` | BIGINT, no | Parent category |
| `name` | VARCHAR(180), no | Product name |
| `description` | VARCHAR(1000), no | Product description |
| `status` | VARCHAR(20), no | Product sale state |
| `created_at` | TIMESTAMP, no, current timestamp | Creation time |
| `product_code` | VARCHAR(80), yes | Showcase product code |
| `cover_image_path` | VARCHAR(255), yes | Approved product-level default asset path |

Boundary: no writing UI in P2/P3.

## `product_sku` - 商品 SKU

Responsibility: purchasable variant and current SKU asset. Primary key: `id BIGINT`; foreign key: `product_id -> product.id`; unique: `sku_code`. Source: CommerceFlow原创, independently compared with mall/litemall.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | SKU id |
| `product_id` | BIGINT, no | Parent product |
| `sku_code` | VARCHAR(80), no | Unique business SKU code |
| `color` | VARCHAR(60), no | Variant color |
| `size` | VARCHAR(40), no | Variant size |
| `sale_price` | DECIMAL(19,2), no | Current sale price |
| `currency` | VARCHAR(10), no, `CNY`; V9 check requires `CNY` | Price currency |
| `status` | VARCHAR(20), no | SKU sale state |
| `image_path` | VARCHAR(255), yes | Current approved SKU asset path |

Boundary: this is the current SKU asset path. New order history uses the separate `order_item.image_path_snapshot`; old orders are not backfilled.

## `inventory` - 可用库存

Responsibility: current stock fact for one SKU. Primary key: `id BIGINT`; foreign key: `sku_id -> product_sku.id`; unique: `sku_id`; check: `available_stock >= 0`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Inventory id |
| `sku_id` | BIGINT, no | One-to-one SKU link |
| `available_stock` | INT, no | Non-negative available quantity |
| `updated_at` | TIMESTAMP, no, current timestamp | Last stock write time |

## `cart_item` - 购物车项

Responsibility: selected SKU and quantity for a demo user. Primary key: `id BIGINT`; FKs: `user_id -> user_account.id`, `sku_id -> product_sku.id`; unique: `(user_id, sku_id)`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Cart item id |
| `user_id` | BIGINT, no | Owning user |
| `sku_id` | BIGINT, no | Selected SKU |
| `quantity` | INT, no | Positive requested quantity |
| `selected` | BOOLEAN, no, true | Checkout selection |
| `created_at` | TIMESTAMP, no, current timestamp | Creation time |

## `orders` - 订单头

Responsibility: successful creation result and idempotency record. Primary key: `id BIGINT`; FK: `user_id -> user_account.id`; uniques: `order_no`, `(user_id, idempotency_key)`. Source: CommerceFlow原创; simplified independently after reference research.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Internal order id |
| `order_no` | VARCHAR(40), no | Unique business order number |
| `user_id` | BIGINT, no | Ordering user |
| `idempotency_key` | VARCHAR(120), no | HTTP request idempotency identity |
| `request_fingerprint` | VARCHAR(128), no | Request-body conflict comparison digest |
| `total_amount` | DECIMAL(19,2), no | BigDecimal-backed order total |
| `currency` | VARCHAR(10), no; V9 check requires `CNY` | Order currency |
| `status` | VARCHAR(30), no | P3 currently uses `CREATED` only |
| `created_at` | TIMESTAMP, no, current timestamp | Successful creation time |

Boundary: no payment, delivery, logistics, refund, or completed states.

## `order_item` - 订单商品快照

Responsibility: immutable business snapshots for each order line. Primary key: `id BIGINT`; FK: `order_id -> orders.id`. Source: CommerceFlow原创, snapshot-field idea compared with litemall/mall.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Item id |
| `order_id` | BIGINT, no | Parent order |
| `product_id` | BIGINT, no | Product reference at creation |
| `sku_id` | BIGINT, no | SKU reference at creation |
| `product_name_snapshot` | VARCHAR(180), no | Historical product name |
| `sku_code_snapshot` | VARCHAR(80), no | Historical SKU code |
| `sku_attributes_snapshot` | VARCHAR(180), no | Historical combined attributes |
| `color_snapshot` | VARCHAR(60), yes | Historical color |
| `size_snapshot` | VARCHAR(40), yes | Historical size |
| `image_path_snapshot` | VARCHAR(255), yes | Asset path captured at successful order creation; `NULL` is supported for pre-V7 orders |
| `unit_price` | DECIMAL(19,2), no | Historical unit price |
| `quantity` | INT, no; V9 check requires `> 0` | Purchased quantity |

Boundary: this records a local path, not an immutable binary, CDN version, or asset lifecycle. The P3 page renders only this snapshot path and does not join the current SKU image as a fallback.

## `inventory_movement` - 库存变动证据

Responsibility: transaction-bound stock deduction evidence. Primary key: `id BIGINT`; FKs: `order_no -> orders.order_no`, `sku_id -> product_sku.id`; index: `idx_inventory_movement_order_no`; unique: `(order_no, sku_id, movement_type)`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Movement id |
| `order_no` | VARCHAR(40), no | Related business order |
| `sku_id` | BIGINT, no | Related SKU |
| `movement_type` | VARCHAR(30), no | Current P3 deduction type |
| `quantity` | INT, no | Positive deducted amount |
| `stock_before` | INT, no | Stock before deduction |
| `stock_after` | INT, no | Stock after deduction |
| `idempotency_key` | VARCHAR(120), no | Request identity for evidence |
| `created_at` | TIMESTAMP, no, current timestamp | Evidence time |

Checks: `quantity > 0`, `stock_before >= 0`, `stock_after >= 0`.

## `inventory_change_log` - 旧库存日志

Responsibility: V1 legacy log shape. Primary key: `id BIGINT`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Log id |
| `sku_id` | BIGINT, no | SKU reference without FK |
| `change_type` | VARCHAR(30), no | Change category |
| `quantity` | INT, no | Change quantity |
| `order_no` | VARCHAR(40), no | Related order number |
| `created_at` | TIMESTAMP, no, current timestamp | Record time |

Boundary: not used by current P3. Retention/removal needs a future decision.

## `ai_trace` - AI 调用证据

Responsibility: AI response/evidence trace. Primary key: `id BIGINT`; unique: `trace_id`. Source: CommerceFlow原创.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Trace id |
| `trace_id` | VARCHAR(60), no | Unique external trace identifier |
| `user_id` | BIGINT, yes | Optional demo user reference |
| `question` | VARCHAR(500), no | Submitted question |
| `provider_mode` | VARCHAR(30), no | Mock or compatible provider mode |
| `answer` | VARCHAR(2000), no | Structured-answer representation |
| `evidence_json` | VARCHAR(4000), no | Evidence payload |
| `status` | VARCHAR(30), no | Trace result state |
| `created_at` | TIMESTAMP, no, current timestamp | Trace creation time |

## `outbox_event` - 事务 Outbox 事件

Responsibility: store a business event in the same database transaction as its order write so a later worker can deliver it without losing the event between a commit and a publish attempt. Primary key: `id BIGINT`; unique: `event_id`; indexes: pending scheduling and aggregate lookup. Source: CommerceFlow原创, structure compared with transactional-outbox references.

| Field | Type / null / default | Meaning |
| --- | --- | --- |
| `id` | BIGINT, no, auto increment | Internal event row id |
| `event_id` | VARCHAR(120), no, unique | Deterministic event identity |
| `aggregate_type` | VARCHAR(80), no | Current aggregate category, `ORDER` |
| `aggregate_id` | VARCHAR(80), no | Current order number |
| `event_type` | VARCHAR(120), no | Current event, `ORDER_CREATED` |
| `payload_json` | VARCHAR(4000), no | Bounded serialized event facts |
| `status` | VARCHAR(20), no, `PENDING` | Delivery lifecycle placeholder |
| `attempts` | INT, no, `0` | Future delivery-attempt counter |
| `available_at` | TIMESTAMP, no, current timestamp | Earliest future delivery time |
| `last_error` | VARCHAR(500), yes | Bounded delivery error summary |
| `created_at` | TIMESTAMP, no, current timestamp | Event creation time |
| `published_at` | TIMESTAMP, yes | Future successful-delivery time |

Boundary: E2 only appends a `PENDING` `ORDER_CREATED` event. There is no broker, worker, notification side effect, retry scheduler, or claim that an external consumer has received the event yet.
