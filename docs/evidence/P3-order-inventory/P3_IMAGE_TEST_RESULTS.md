# P3.1 Image Snapshot Test Results

## Automated Results

| Area | Command | Result |
| --- | --- | --- |
| Java | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 14 passed, 0 failures, 0 errors |
| Vue tests | `npm.cmd run test -- --run` in `apps/admin-web` | 3 files, 13 tests passed |
| Vue production build | `npm.cmd run build` in `apps/admin-web` | `vue-tsc --noEmit` and Vite build passed |

The Java suite applies Flyway V1-V7 to a clean H2 test database. It covers image snapshot persistence, the corrected Product/SKU ids, `BigDecimal` totals, duplicate-SKU aggregation, shortage rollback, replay, conflict, MyBatis evidence assembly, a two-product order, and a legacy row with `NULL image_path_snapshot`.

The Vue suite covers loading, empty, error and retry states; order selection; two-product images and item count; stored API paths; missing snapshots; and image load failure while preserving item text.

## Clean MySQL and Real API Verification

`scripts/start-mysql.ps1 -Reset` recreated local MySQL from an empty volume. Flyway applied V1 through V7. The P3.1 Java API ran on local port 8082 because existing local processes already occupied 8080 and 8081; no process was stopped or modified.

| Scenario | Real result |
| --- | --- |
| Two-product order | `CF1785054984427`, SKU 10004 plus SKU 10003, total `328 CNY`, two item snapshots, two movements |
| Single-item order | `CF1785054984544`, SKU 10002, one item snapshot and one movement |
| Same key, same body | Returned `CF1785054984427`; no new order, item, snapshot, or movement |
| Same key, different body | HTTP 409 `IDEMPOTENCY_KEY_REUSED`; no write |
| Insufficient stock | HTTP 400 `INVENTORY_INSUFFICIENT`; no order or movement |

The two-product evidence response returned `/assets/products/product-tshirt-gray.png` and `/assets/products/product-tote-beige.png`. Direct database/API verification confirmed the new order item product ids are `101` and `102`, rather than their SKU ids.

## Browser Acceptance

The capture used the running Vue source at `http://127.0.0.1:5174`, `1920 x 1080`, device scale factor `1`, and `zh-CN`. Its pre-existing local development API base targeted occupied port 8081, so the isolated browser verification route forwarded only API requests to the current P3.1 Java process at 8082. The browser received real MySQL-backed responses; source configuration was not changed and no existing process was interrupted.

The selected two-product order showed two item rows, two movement rows, `¥328.00`, stored product thumbnails, and a collapsed developer-information section. Browser console errors: `0`; page errors: `0`; local product asset requests: HTTP 200.

## Observed Issues

1. The manually inserted legacy test order number exceeded the existing `orders.order_no VARCHAR(40)` limit. The test fixture was shortened to a valid 38-character value; no schema change was needed.
2. The bundled Playwright browser executable was unavailable. Installed Chrome was used for the reproducible local capture; no dependency was installed.
