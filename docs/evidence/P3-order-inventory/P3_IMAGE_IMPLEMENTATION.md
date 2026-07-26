# P3.1 Order Item Image Snapshot Implementation

## Scope

P3.1 makes the order evidence page image-aware without changing the P3 order lifecycle. The page remains read-only and supports only `CREATED / 已创建` orders. Product assets are original local Showcase assets, not third-party commerce data.

## Forward-Only Schema Change

`V7__add_order_item_image_snapshot.sql` adds nullable `order_item.image_path_snapshot VARCHAR(255)`. Earlier Flyway migrations were not edited. `NULL` is intentionally supported for orders that predate V7.

## Transactional Write and Correctness Repair

`OrderService.submit` resolves each SKU before writing the order. It saves the resolved SKU `image_path` together with the existing product name, SKU code, attributes, price, and quantity snapshots. This happens in the existing `@Transactional` path with the conditional inventory update, `orders`, `inventory_movement`, and cart cleanup.

During the audit, the OrderItem insert was found to pass `sku.id()` into the `product_id` position. The root cause was a repository method signature that accepted the product id separately while the caller reused the SKU value. P3.1 moves the resolved product id and SKU id into the same `ResolvedLine` record. New orders therefore write the actual parent product id and SKU id; the two-product regression test proves `10004 -> 101` and `10003 -> 102`.

If an inventory update cannot satisfy `available_stock >= quantity`, the service throws before an order, item, image snapshot, or movement can be committed. Any later failure rolls back all of those writes as one transaction. A same-key replay returns the original order and makes no new snapshot or movement; a same-key different body returns HTTP 409 without new writes.

## Read Models and Page

- `GET /api/orders?userId=1` returns summary item snapshots including `imagePathSnapshot`.
- `GET /api/orders/{orderNo}` returns the stored order item data.
- `GET /api/orders/{orderNo}/execution-evidence` uses the MyBatis result map to return stored items and movements.

The MyBatis nested result map identifies items by `item_id` and movements by `movement_id`. The real two-product order returns exactly two items and two movement rows without Java-side or Vue-side deduplication.

The Vue page displays an order-card thumbnail, first product snapshot, item count, item-table thumbnails, and fixed-size missing/failed-image states. It renders only the returned snapshot path. No current-SKU lookup, reference image background, fake inventory value, or write control was added.

## Remaining Boundary

The snapshot is a path, not immutable media storage. File deletion, object-storage migration, CDN versioning, payment, shipment, logistics, refunds, and multi-node in-progress idempotency are out of scope.
