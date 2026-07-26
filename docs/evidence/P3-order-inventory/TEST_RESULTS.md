# P3 Test Results

## Java

Command:

```powershell
./mvnw.cmd -f apps/mall-api/pom.xml test
```

Result on 2026-07-26: 12 tests passed, 0 failures, 0 errors.

Coverage includes successful order snapshots and `BigDecimal` total, conditional stock deduction, movement before/after values, shortage/no-residue behavior, full rollback after a prior deduction, same-key replay, same-key conflict HTTP 409, MyBatis mapper/result map, evidence API, duplicate-SKU aggregation, database movement uniqueness, and clean H2 Flyway V1-V6 migration.

## Vue

Command:

```powershell
npm.cmd run test -- --run
npm.cmd run build
```

Result: 3 Vitest files and 11 tests passed; `vue-tsc --noEmit` and Vite production build passed.

Coverage includes order list/detail/evidence rendering, list loading/empty/error/retry states, order-number search, selection, item snapshots, movement facts, and idempotency details.

## Real Runtime

- `scripts/start-mysql.ps1 -Reset` rebuilt MySQL from an empty volume.
- Flyway applied V1 through V6 successfully.
- `scripts/seed-order-evidence.ps1` created three successful orders through the real API and asserted replay, conflict, shortage, and primary movement facts.
- Browser verification used `1920 x 1080`, `deviceScaleFactor: 1`, and `zh-CN`; it checked real API data, order-number search, and zero console/page errors.
