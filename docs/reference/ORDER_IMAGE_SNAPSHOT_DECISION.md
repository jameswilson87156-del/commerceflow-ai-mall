# Order Image Snapshot Decision

**Decision status:** IMPLEMENTED_P3.1
**Decision:** Option B is implemented: persist `order_item.image_path_snapshot` at successful order creation.

## Current Fact

P3.1 adds V7 with a nullable `order_item.image_path_snapshot`. The write path copies the resolved SKU `image_path` into the OrderItem record in the same transaction as the order, stock deduction, movement evidence, and cart cleanup. Summary and MyBatis evidence APIs return the stored field; the Vue page renders no live-SKU image fallback.

## Options

| Option | Description | Historical consistency | Cost / risk | Decision |
| --- | --- | --- | --- | --- |
| A | At read time, join to the SKU's current `image_path`. | Weak: a later SKU image change changes historical order presentation. | Low schema cost, but hides history and breaks deleted/migrated asset cases. | Reject for P3.1 |
| B | Copy the selected SKU `image_path` to `order_item.image_path_snapshot` in the existing order transaction. | Good for the local showcase: the order retains the path used at creation. | One nullable field, DTO/mapper/API/test additions, and a missing-image UI state. | Implemented |
| C | Persist an asset id, snapshot path, content/version metadata, and retention lifecycle. | Stronger across object-storage/CDN migrations. | High MVP cost; requires asset-versioning and deletion policy that CommerceFlow does not have. | Defer |

## Why Option B Fits P3.1

- It matches the existing snapshot pattern (`product_name_snapshot`, `sku_code_snapshot`, `color_snapshot`, `size_snapshot`, `unit_price`).
- It makes a screenshot defensible: image data originates from the successful order transaction and evidence API, not a front-end guess.
- It keeps the database/API simple while preserving an interview-ready explanation of historical read models.
- A path snapshot does not solve a later physical-file deletion or CDN migration. P3.1 explicitly treats that as a future asset-management concern, not a claim that the path is immutable forever.

## Implemented Boundary

1. V7 adds `image_path_snapshot VARCHAR(255)` without editing V1-V6.
2. `OrderService` saves the approved live SKU path with the other item snapshots, then exposes it through the summary and evidence read models.
3. The order write path was also corrected to persist the resolved parent `product_id`, rather than accidentally writing `sku_id` into the product column.
4. Automated tests cover snapshot persistence, replay, rollback, two-product MyBatis assembly, legacy `NULL`, and missing/failed local image states.
5. The page renders only the stored API snapshot. It does not look up the live SKU path.
