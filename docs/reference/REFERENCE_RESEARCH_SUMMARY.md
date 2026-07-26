# E-commerce Reference Research Summary

## Conclusions

1. **Why P3 has no image:** `order_item` currently stores text and price snapshots only. The P3 evidence DTO, MyBatis result map, and Vue type have no image field, so a real image cannot be displayed without guessing from current SKU data.
2. **Image snapshot:** recommend adding `order_item.image_path_snapshot` in P3.1, saved in the existing order transaction. This is Option B in `ORDER_IMAGE_SNAPSHOT_DECISION.md`.
3. **Schema naming:** no broad rename is justified. Keep the concise current names; add the explicit snapshot field only. `inventory_change_log` needs a later lifecycle decision because `inventory_movement` is the active evidence table.
4. **Keep:** `product`, `product_sku`, `product_code`, `sku_code`, `orders`, `order_item`, `order_no`, `idempotency_key`, `request_fingerprint`, `inventory`, `inventory_movement`, `quantity`, `stock_before`, `stock_after`, `created_at`, `updated_at`, `cover_image_path`, and SKU `image_path`.
5. **P3.1 addition:** `order_item.image_path_snapshot VARCHAR(255)` plus read-model/API/UI/test support. No table prefix, status rename, payment state, or asset-version table.
6. **Best reference order:** first `litemall` for compact order-item snapshot modelling; then `macrozheng/mall` and `mall-admin-web` for product/SKU and detail information architecture; then `crmeb_java` for broader organisation. NewBee projects are design-only comparisons.
7. **View-only projects:** both GPL NewBee repositories are Tier B and cannot supply code, SQL, templates, assets, or screenshots.
8. **Permitted images:** only CommerceFlow's five approved original AI-generated local product assets. All reference-project images and screenshots are research-only.
9. **P3.1 page:** left order cards with a first-item thumbnail and count; right basic facts, image-aware OrderItem table, inventory evidence, idempotency result, and collapsed development notes.
10. **Minimum next-stage change list:** new migration; order creation snapshot write; DTO/repository/MyBatis/API/type propagation; Vue rendering/states; Java/Vue tests; real screenshot; evidence update. User approval is required before implementation.

## Reference Scope

Research findings are recorded with actual branch/SHA/license evidence in `OPEN_SOURCE_LICENSE_MATRIX.md` and `SOURCE_PROVENANCE.md`. CommerceFlow did not import reference repositories, databases, dependencies, source code, or assets.
