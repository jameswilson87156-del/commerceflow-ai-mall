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
- database: clean local MySQL migrated by Flyway V1-V5
- visible list: three orders created by `scripts/seed-order-evidence.ps1`
- selected order: `CF1785047035633`
- visible movement: SKU `10004`, `28 -> 27`, quantity `1`
- browser console errors: none
- page errors: none

The screenshot shows only `CREATED / 已创建`; it does not imply payment, shipment, logistics, refund, or completion.
