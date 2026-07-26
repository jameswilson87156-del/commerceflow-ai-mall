# Screenshot Evidence

## Artifact

`screenshots/v2/02-product-sku-real.png`

The image was captured from the running Vue admin page using the local API. It is a real runtime screenshot, not an AI design reference and not an image embedded in the page.

## Capture Context

- Vue URL: `http://127.0.0.1:5174/`
- API base: `http://localhost:8081/api`
- Data source: local MySQL populated by the project Flyway demo data, including the current runtime inventory values.
- Selected product in the screenshot: `Essential Cotton Shirt` (`productId: 101`).
- Visible SKU codes: `SHIRT-BLK-M` and `SHIRT-WHT-L`.
- Browser console errors at capture: none.

The capture represents a desktop layout. The retained image is `1685 x 1080` physical pixels because the local in-app browser capture applies desktop display scaling; the page itself was verified in the desktop layout and was not cropped or stretched.

## Commands Recorded Before Capture

```powershell
./scripts/start-mysql.ps1
./mvnw.cmd -f apps/mall-api/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
npm run build
npm run dev -- --host 127.0.0.1
```

`npm test` and `npm run build` completed successfully before capture.

## P2.1 Candidate Artifact

`screenshots/v2/02-product-sku-real-v2.png`

The previous `02-product-sku-real.png` remains unchanged as the P2 record. The P2.1 file is a separate candidate for human review and does not replace the earlier evidence.

Capture settings:

- viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- locale and document language: `zh-CN`
- runtime: locally installed Google Chrome controlled through Playwright
- local Vue page: `http://127.0.0.1:5174/`
- API base: `http://localhost:8081/api`
- browser console errors: none

The candidate contains only data returned by the rebuilt local MySQL/Flyway environment: two Products and three SKUs. It is not an AI design reference and is not used as a page background.

## P2 Final Primary Artifact

`screenshots/v2/02-product-sku-real-final.png`

This is the formal primary screenshot for the Product and SKU page after the final Chinese label lock. It was captured from the same real local API using the two localized Products and three localized SKUs.

The earlier `02-product-sku-real.png` and `02-product-sku-real-v2.png` remain historical evidence and are intentionally retained.

Final capture settings:

- viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- locale and document language: `zh-CN`
- runtime: locally installed Google Chrome controlled through Playwright
- browser console errors: none

## P2 Design-Lock Real Runtime Artifact

`screenshots/v2/02-product-sku-design-lock-real.png`

This is the real runtime screenshot for the reference-driven rework. It is not an AI design reference, does not embed `target-page.png`, and uses original local product assets served from `apps/admin-web/public/assets/products/`.

- viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- locale: `zh-CN`
- Vue URL: `http://127.0.0.1:5174/`
- Java API: `http://localhost:8081/api`
- database: local MySQL rebuilt through Flyway V1-V4
- visible data: `PROD-1001`, 4 real T-shirt SKUs, and stock `96`, `182`, `28`, `0`
- selected SKU at capture: `T-SHIRT-GRAY-L`; its gray product asset was returned by the API and displayed by the page
- console errors: none; page errors: none

The target reference remains only in `docs/design_refs/source/product-sku/`. It is never used as a page background or runtime evidence.
