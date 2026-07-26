# P3 Test Results

## Java

Command:

```powershell
./mvnw.cmd -f apps/mall-api/pom.xml test
```

Result on 2026-07-26: 14 tests passed, 0 failures, 0 errors.

Coverage includes successful order snapshots and `BigDecimal` total, the P3.1 image-path snapshot, corrected parent Product id, conditional stock deduction, movement before/after values, shortage/no-residue behavior, full rollback after a prior deduction, same-key replay, same-key conflict HTTP 409, MyBatis mapper/result map, a multi-product evidence response, legacy null snapshots, duplicate-SKU aggregation, database movement uniqueness, and clean H2 Flyway V1-V7 migration.

## Vue

Command:

```powershell
npm.cmd run test -- --run
npm.cmd run build
```

Result: 3 Vitest files and 13 tests passed; `vue-tsc --noEmit` and Vite production build passed.

Coverage includes order list/detail/evidence rendering, list loading/empty/error/retry states, order-number search, selection, stored image paths, missing and failed image states, two-product item snapshots, movement facts, and idempotency details.

## Real Runtime

- `scripts/start-mysql.ps1 -Reset` rebuilt MySQL from an empty volume.
- Flyway applied V1 through V7 successfully.
- Real API verification created a two-product order (`10004` plus `10003`, `328 CNY`) and a single-item order, then proved replay, conflict, shortage, and stored image paths.
- Browser verification used `1920 x 1080`, `deviceScaleFactor: 1`, and `zh-CN`; it checked real API data, two snapshots, two movement rows, product assets, and zero console/page errors.
