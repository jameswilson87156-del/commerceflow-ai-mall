# P6B.1 Implementation

## Scope

P6B.1 hardens the mobile showcase image path and applies the shared mobile visual system to the real product, cart, checkout, order result, and order detail flows. It does not change Java business logic, Flyway migrations, order transaction behavior, inventory behavior, idempotency, Redis, Python, or the P2-P5 pages.

## Runtime asset rule

The five approved local product images are copied to `apps/mobile-app/src/static/assets/products/`. The Java API remains the source of image fields:

- Product: `coverImagePath`
- SKU: `imagePath`
- CartItem: `imagePath`
- OrderItem: `imagePathSnapshot`

The shared `resolveImageUrl` helper maps API values such as `/assets/products/product-tshirt-gray.png` to `/src/static/assets/...` during Vite development and `/static/assets/...` in the built H5 output. `ProductImage.vue` owns load failure handling and renders a neutral placeholder after an image error.

## Pages and components

- `MobileHeader.vue`: compact title, subtitle, back, and action header.
- `ProductImage.vue`: central image rendering, reset-on-source-change, error placeholder.
- Product list: real Product API data and local images.
- Product detail: real SKU selection, image switching, unavailable SKU guard, stock facts, and server cart add.
- Cart: real cart read/update/delete operations and real item images.
- Order confirm: real cart snapshot and real server order creation.
- Order result: real order number, amount, item count, creation time, and `CREATED` status.
- Order detail: real `imagePathSnapshot` values and historical item fields.

The page remains read-only for catalog data. The only writes remain the existing cart and order operations already provided by the P6B backend.

## Visual system

The mobile theme uses a pale page background, white panels, blue primary actions, green success inventory, orange warning inventory, red error states, safe-area padding, fixed bottom actions, and stable image dimensions. H5 styles use deterministic `px` values so the 390x844 and 430x932 browser checks do not depend on an unprocessed `rpx` unit.

