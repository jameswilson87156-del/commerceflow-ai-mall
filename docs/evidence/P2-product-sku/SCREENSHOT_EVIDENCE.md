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
