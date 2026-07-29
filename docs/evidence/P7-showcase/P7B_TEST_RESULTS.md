# P7B Test Results

Run on 2026-07-29 from the module directories.

| Module | Command | Result |
| --- | --- | --- |
| Java | `mvn test` in `apps/mall-api` | 45 passed, 0 failed, including Redis integration tests |
| Python | `.venv\\Scripts\\python.exe -m pytest` in `services/ai-service` | 11 passed |
| Admin | `npm test` in `apps/admin-web` | 45 passed |
| Admin | `npm run build` in `apps/admin-web` | Type check and production build passed |
| Mobile | `npm test` in `apps/mobile-app` | 31 passed |
| Mobile | `npm run build` in `apps/mobile-app` | H5 build passed; existing dynamic-import advisory only |
| Mobile | `npm run build:uni` in `apps/mobile-app` | Non-H5 compile passed; existing UniApp update and dynamic-import advisories only |

The Java suite initially exposed cross-test `ai_trace` leakage in `OperationsOverviewApiTests`. Each overview test now deletes only its own test trace data before asserting an empty trace aggregate. The final full Java run passed.
