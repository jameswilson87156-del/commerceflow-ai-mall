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
