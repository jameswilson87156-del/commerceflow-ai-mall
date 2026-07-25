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

`apps/admin-web/src/catalog.ts` defines the single `LOW_STOCK_THRESHOLD` constant as `5`.

- `availableStock === 0`: `缺货`
- `availableStock > 0 && availableStock < 5`: `库存偏低`
- otherwise: `库存正常`

This is a presentation-only label. `availableStock` remains the real inventory fact returned by Java; the Vue client has no inventory write action.

## Selection Failure Handling

The selected product identifier is kept separately from the successfully loaded detail object. This keeps the selected list item and any retry target aligned with the product the user clicked, even if a later detail request fails.
