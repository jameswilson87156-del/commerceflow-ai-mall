# P6B.1 Test Results

## Automated regression

| Area | Command | Result |
| --- | --- | --- |
| Java | `cmd /c mvnw.cmd -f apps/mall-api/pom.xml test` | 42 passed, 0 failed |
| Redis integration | Included in Java run, `AiRateLimitRedisIntegrationTests` | 5 passed |
| Python | `python -m pytest -q` from `services/ai-service` | 11 passed |
| Admin Vue | `npm test` from `apps/admin-web` | 43 passed |
| Admin build | `npm run build` from `apps/admin-web` | Passed, type check and Vite build |
| Mobile unit/runtime | `npm test` from `apps/mobile-app` | 6 passed |
| Mobile H5 | `npm run build` from `apps/mobile-app` | Passed |
| UniApp non-H5 | `npm run build:uni` from `apps/mobile-app` | Passed |

Java tests validated a clean in-memory Flyway sequence V1-V8. A separate temporary MySQL database was created for a clean-start check, but the ad-hoc Spring Boot process did not become healthy within the bounded 90-second wait. The temporary database was removed immediately and this is not counted as a MySQL clean-start pass.

## Browser acceptance

- H5 dev server: `http://127.0.0.1:5176`
- Java API: `http://127.0.0.1:8081`
- MySQL container: healthy on local port 3307
- Viewports: 390x844 and 430x932, device scale factor 1, locale `zh-CN`
- Routes checked: product list, product detail, cart, order confirm, order result, order detail
- 390px final result page overflow: 0px after the result action button fix
- 390px and 430px final page overflow: 0px
- Browser console errors: 0
- Page errors: 0

Manual browser coverage included real image loading, SKU selection, unavailable-SKU disabling, real cart add, real two-item checkout, real order creation, and historical order image snapshots.

