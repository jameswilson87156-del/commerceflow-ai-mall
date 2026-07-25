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
