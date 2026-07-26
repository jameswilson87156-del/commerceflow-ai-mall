# Test Results

## Automated Frontend Tests

Command: `npm test` in `apps/admin-web`

Result: passed on 2026-07-25.

- 2 test files passed.
- 6 tests passed.
- Covered stock labels and the centralized threshold, name/SKU/category/inventory filtering, loading state, list error state, empty state, product selection, and SKU table rendering.

## Production Build

Command: `npm run build` in `apps/admin-web`

Result: passed on 2026-07-25.

The command completed `vue-tsc --noEmit` and the Vite production build successfully.

## Local API Verification

The local API process was checked through `GET /actuator/health`, which returned `UP`.

`GET /api/products` returned two real Flyway-backed demo products. At verification time they exposed three total SKUs:

- `SHIRT-BLK-M`: available stock `11`
- `SHIRT-WHT-L`: available stock `8`
- `TOTE-TAN-ONE`: available stock `4`

The page also requested `GET /api/products/101` and `GET /api/products/102` when selecting the corresponding real product.

## Browser Verification

On the running Vue page, the following interactions were verified against the local API data:

- loading completes into the two-product list;
- switching from Product `101` to Product `102` loads its one-SKU detail;
- searching `TOTE-TAN-ONE` leaves the matching real SKU/product visible;
- stock `4` is labeled `库存偏低` according to the threshold;
- the final capture has no browser console errors.

## P2.1 Verification

Date: 2026-07-25.

- Database rebuilt from a clean Docker volume using `scripts/start-mysql.ps1 -Reset`.
- MySQL Flyway history shows migrations `V1`, `V2`, and `V3` as successful.
- Java tests: 4 passed. The H2 test database also migrated from an empty schema through `V3`.
- Vue Vitest: 2 files and 6 tests passed, including Chinese product/category fixtures, filtering, loading, empty, error, selection, and SKU rendering.
- Vue production build passed.
- `GET /api/products`, `GET /api/products/101`, and `GET /api/products/102` returned the localized real data.
- Browser verification against the local API passed for Chinese search, category filtering, inventory filtering, product switching, and console-error inspection.

After the clean reset, real baseline inventory returned as `12`, `8`, and `5` for SKUs `10001`, `10002`, and `10003`. These quantities were not changed by the localization migration.

## P2 Design-Lock Verification

Date: 2026-07-26.

- MySQL was rebuilt from an empty Docker volume with `scripts/start-mysql.ps1 -Reset`. Flyway applied V1 through V4 successfully.
- Java: `./mvnw.cmd -f apps/mall-api/pom.xml test` passed: 6 tests, 0 failures, 0 errors. The added catalog API tests validate image-path fields, the four T-shirt SKUs, low stock `28`, and out-of-stock `0` on a clean H2/Flyway schema.
- Vue: `npm.cmd run test -- --run` passed: 2 files and 6 tests. The tests cover the centralized stock threshold, name/SKU/category/state filters, loading, empty, error/retry, product switching, and rendered image paths.
- Vue: `npm.cmd run build` passed (`vue-tsc --noEmit` and Vite production build).
- Local API: `GET /api/products` returned 2 Products; Product `101` returned four SKUs with stocks `96`, `182`, `28`, and `0`; `GET /api/products/102` returned its tote SKU and image path.
- Browser: local Chrome + bundled Playwright runtime used viewport `1920 x 1080`, `deviceScaleFactor: 1`, and `zh-CN`. It verified real API data, product-name search, SKU-code search, low-stock filtering, category filtering, product/SKU switching, and a zero console/page-error count.
