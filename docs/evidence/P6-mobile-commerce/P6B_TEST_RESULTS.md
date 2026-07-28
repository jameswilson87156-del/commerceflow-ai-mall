# P6B Test Results

Date: 2026-07-29

## Automated Results

| Area | Command | Result |
|---|---|---|
| Java API | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 43 passed, 0 failures, 0 errors |
| Redis regression subset | `AiRateLimitRedisIntegrationTests` | 5 passed |
| Python AI service | `pytest -q` | 11 passed |
| Admin Vue | `npm test` | 43 passed |
| Admin Vue type/build | `npm run build` | passed |
| Mobile helper tests | `apps/mobile-app/npm test` | 18 passed |
| Mobile H5 build | `apps/mobile-app/npm run build` | passed |
| Mobile UniApp build | `apps/mobile-app/npm run build:uni` | passed |

Java test reports validated 9 test classes and 43 tests. Flyway applied V1-V8 from a clean H2 test database, and P6B.2 additionally verified V1-V8 on a separate empty MySQL 8.4 volume. The full Java suite includes the five Redis rate-limit integration test methods; the local API browser run used Redis only for existing P5 functionality and did not add a mobile Redis feature.

## Browser Acceptance

- H5 viewport: 390x844, DPR 1.
- Real Java API: `http://127.0.0.1:8081` through the Vite `/api` proxy.
- Real MySQL demo data and real server cart/order writes.
- Product image responses: HTTP 200 for all requested local product assets.
- Console errors during the final five-page flow: 0.
- Created order: `CF1785264511123`, total `328.00 CNY`, status `CREATED`.

## Known Test Limit

The mobile Node suite covers pure runtime and display-policy helpers automatically. The rendered interaction matrix is recorded in `P6B2_TEST_COVERAGE_MATRIX.md` and reproducible Playwright browser evidence rather than pretending Node helpers cover the rendered pages.
