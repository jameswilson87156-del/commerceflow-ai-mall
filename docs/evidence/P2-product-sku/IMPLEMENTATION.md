# Implementation Notes

## Reference Use

The approved V1 design reference informed the dark navigation shell, light content surface, master-detail layout, restrained panels, blue selection treatment, and cyan inventory-success treatment.

The implementation does not use the reference image in the application. It also omits unsupported product images, fabricated SKU counts, large fabricated stock values, KPI cards, product editing, bulk actions, suppliers, sales figures, and marketing actions.

## Real Data Flow

1. `ProductSkuManagement.vue` requests `GET /api/products` when mounted.
2. The returned product list is stored in component state and supplies the real product count, list, category options, and browser-side filters.
3. Selecting a product requests `GET /api/products/{productId}`.
4. The detail response supplies the selected Product fields and the SKU table.
5. `catalog.ts` formats money and centralizes stock display rules.

There is no local fallback product list. An API failure displays an error state; an empty response displays an empty state.

## Local Search and Filters

Search matches real product names and `skuCode` values returned by the list endpoint. Category and inventory-state filters also operate against that same in-memory API response. The controls do not call an unimplemented endpoint and do not simulate a server-side filter.

## Inventory Display Rule

`apps/admin-web/src/catalog.ts` defines the single `LOW_STOCK_THRESHOLD` constant as `30`.

- `availableStock === 0`: `缺货`
- `availableStock > 0 && availableStock < 30`: `库存偏低`
- otherwise: `库存正常`

This is a presentation-only label. `availableStock` remains the real inventory fact returned by Java; the Vue client has no inventory write action.

## Selection Failure Handling

The selected product identifier is kept separately from the successfully loaded detail object. This keeps the selected list item and any retry target aligned with the product the user clicked, even if a later detail request fails.

## P2.1 Localization and Visual Polish

`V3__localize_showcase_catalog.sql` updates only presentation values in the existing records. It preserves the Product IDs, SKU IDs, SKU codes, sale prices, currency, inventory quantities, and the two-product / three-SKU record count.

| Record | Before V3 | After V3 |
| --- | --- | --- |
| Product 101 | Essential Cotton Shirt / Everyday Wear | 轻盈棉质基础T恤 / 日常服饰 |
| Product 102 | Structured Work Tote / Work Essentials | 简约通勤托特包 / 通勤配件 |
| SKU 10001 color | Black | 黑色 |
| SKU 10002 color | White | 白色 |
| SKU 10003 color | Tan | 卡其色 |

The existing size values remain unchanged: `M`, `L`, and `One Size`. SKU codes remain technical identifiers and therefore remain English.

The stylesheet makes the Chinese product copy easier to scan, tightens the panel gaps, increases SKU table readability, and aligns the desktop product-list panel with the selected-detail stack. These are layout-only changes; API requests and read-only behavior are unchanged.

## P2 Design-Lock Rework

`V4__add_showcase_product_images.sql` is appended after V1-V3; none of the executed migrations were changed. It adds `product_code` and `cover_image_path` to `product`, plus `image_path` to `product_sku`. The migration updates the existing two local Showcase products and inserts only the two additional T-shirt variants required by the approved copy specification.

The real seed now exposes two Products and five SKUs:

- `PROD-1001` / `轻盈棉质基础 T 恤`: four SKUs with stock `96`, `182`, `28`, and `0`.
- `PROD-1002` / `简约通勤托特包`: one SKU with stock `128`.

The static images are copied byte-for-byte into `apps/admin-web/public/assets/products/`. Java returns their runtime paths, and the Vue component renders only those API values. The page has no local fallback catalog, no business-number constants, and no write controls.
