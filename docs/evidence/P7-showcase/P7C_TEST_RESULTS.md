# P7C Test Results

Final local regression, 2026-07-29:

| Area | Command | Result |
| --- | --- | --- |
| Repository integrity | `python scripts/ci/verify_repository.py` | Passed: README links, canonical files/dimensions, freeze records, tracked-file policy and obvious-key patterns. |
| Java | `mvn test` in `apps/mall-api` | 45 passed, 0 failed. |
| Redis integration | Included in Java suite | 5 real Redis integration tests passed. |
| Python | `.venv\\Scripts\\python.exe -m pytest` in `services/ai-service` | 11 passed. |
| Admin | `npm test` in `apps/admin-web` | 45 passed. |
| Admin build | `npm run build` in `apps/admin-web` | Type check and production build passed. |
| Mobile | `npm test` in `apps/mobile-app` | 31 passed. |
| Mobile H5 | `npm run build` in `apps/mobile-app` | Passed. |
| Mobile non-H5 | `npm run build:uni` in `apps/mobile-app` | Compilation passed; it is not device-runtime evidence. |
| MySQL/Flyway smoke | Isolated MySQL 8.4 + Redis + Java | V1–V8 rows `8`; overview returned products `2`, SKUs `5`, orders `0`, amount `0.00`, AI interactions `0`. |
| CI workflow structure | Python YAML parse and local inspection | Parsed successfully; five intended jobs present. |

The mobile build printed existing UniApp update and dynamic-import advisories. They did not fail the build and do not establish non-H5 runtime support.
