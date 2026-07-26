# P3 Screenshot Evidence

## Artifact

`screenshots/v2/03-order-inventory-real.png`

This screenshot is a real local Vue runtime, not the AI design reference and not a page background.

## Capture Context

- viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- locale: `zh-CN`
- Vue: `http://127.0.0.1:5174/`
- Java API: `http://localhost:8081/api`
- database: clean local MySQL migrated by Flyway V1-V6
- visible list: three orders created by `scripts/seed-order-evidence.ps1`
- selected order: `CF1785048273842`
- visible movement: SKU `10004`, `28 -> 27`, quantity `1`
- browser console errors: none
- page errors: none

## P3.1 Image Snapshot Artifact

`screenshots/v2/03-order-inventory-image-snapshot-real.png`

This is the newer real local P3.1 runtime capture. It does not replace the earlier P3 evidence above.

- viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- locale: `zh-CN`
- Vue source: `http://127.0.0.1:5174/`
- Java API facts: local MySQL-backed API on `http://127.0.0.1:8082/api`
- database: clean local MySQL migrated by Flyway V1-V7
- visible list: two successful P3.1 demo orders
- selected order: `CF1785054984427`
- selected order: two stored item image paths, SKU `10004` and SKU `10003`, total `328 CNY`
- visible movements: two database records, one per selected SKU
- browser console errors: `0`
- page errors: `0`

The image paths are stored `order_item` snapshots returned by the real APIs. The reference image is not used as a page background or as runtime evidence.

The screenshot shows only `CREATED / 已创建`; it does not imply payment, shipment, logistics, refund, or completion.
