# Order Image Snapshot Decision

**Decision status:** RECOMMENDED_PENDING_P3.1_IMPLEMENTATION
**Recommendation:** Use **Option B**: persist `order_item.image_path_snapshot` at successful order creation.

## Current Fact

P3 stores product name, SKU code, color, size, unit price, and quantity snapshots in `order_item`, but no image path. Its evidence query joins `order_item` and `inventory_movement`; it does not join `product` or `product_sku` for an image. Therefore the real P3 page has no order image by design, rather than a missing front-end decoration.

## Options

| Option | Description | Historical consistency | Cost / risk | Decision |
| --- | --- | --- | --- | --- |
| A | At read time, join to the SKU's current `image_path`. | Weak: a later SKU image change changes historical order presentation. | Low schema cost, but hides history and breaks deleted/migrated asset cases. | Reject for P3.1 |
| B | Copy the selected SKU `image_path` to `order_item.image_path_snapshot` in the existing order transaction. | Good for the local showcase: the order retains the path used at creation. | One nullable/required field, DTO/mapper/API/test additions, and a missing-image UI state. | Recommend |
| C | Persist an asset id, snapshot path, content/version metadata, and retention lifecycle. | Stronger across object-storage/CDN migrations. | High MVP cost; requires asset-versioning and deletion policy that CommerceFlow does not have. | Defer |

## Why Option B Fits P3.1

- It matches the existing snapshot pattern (`product_name_snapshot`, `sku_code_snapshot`, `color_snapshot`, `size_snapshot`, `unit_price`).
- It makes a screenshot defensible: image data originates from the successful order transaction and evidence API, not a front-end guess.
- It keeps the database/API simple while preserving an interview-ready explanation of historical read models.
- A path snapshot does not solve a later physical-file deletion or CDN migration. P3.1 explicitly treats that as a future asset-management concern, not a claim that the path is immutable forever.

## Minimum Future Implementation Boundary

1. New Flyway migration only: add `image_path_snapshot VARCHAR(255)` to `order_item`; do not edit V1-V6.
2. During order creation, read the approved live SKU `image_path` and save it with the other item snapshots in the same transaction.
3. Extend order summary/evidence DTOs, repository query, MyBatis result map, and Vue types.
4. Add tests for successful snapshot persistence, idempotent replay retaining one snapshot, transaction rollback leaving no item/snapshot, and a missing or failed local image UI state.
5. Render only the API-returned snapshot. No fallback join to the live SKU image in the P3 page.
