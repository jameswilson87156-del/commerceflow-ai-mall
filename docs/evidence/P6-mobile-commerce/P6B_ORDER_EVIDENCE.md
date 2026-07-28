# P6B Order Evidence

## Created Order

- Order: `CF1785264511123`
- User: `1` (demo user only)
- Status: `CREATED`
- Total: `328.00 CNY`
- Items: one gray T-shirt and one beige tote bag
- Result: returned by the real order API and rendered by result/detail pages

## Snapshot Evidence

The order detail API returned:

- `productNameSnapshot`
- `skuCodeSnapshot`
- `colorSnapshot`
- `sizeSnapshot`
- `imagePathSnapshot`
- `unitPrice`
- `quantity`

The mobile order detail uses the stored snapshot image path. It does not fall back to the current SKU catalog image when a historical snapshot is absent; it shows `暂无历史图片`.

## Boundary

The page does not claim payment, shipment, logistics, refund, address, coupon, delivery, or completion states.
