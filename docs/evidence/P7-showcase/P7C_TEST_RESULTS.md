# P7C Test Results

Final local regression, 2026-09-02:

| Area | Command | Result |
| --- | --- | --- |
| Repository integrity | `py -3 scripts/ci/verify_repository.py` | Passed: README/architecture links, canonical files/dimensions, freeze records, tracked-file policy and obvious-key patterns. |
| Java | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 53 passed, 0 failed, 0 errors, 0 skipped. |
| Redis integration | Included in Java suite | 5 real Redis integration tests passed. |
| Python | `py -3 -m pytest` in `services/ai-service` | 11 passed. |
| Admin | `npm test` in `apps/admin-web` | 45 passed. |
| Admin build | `npm run build` in `apps/admin-web` | Type check and production build passed. |
| Mobile | `npm test` in `apps/mobile-app` | 31 passed. |
| Mobile H5 | `npm run build` in `apps/mobile-app` | Passed. |
| Mobile non-H5 | `npm run build:uni` in `apps/mobile-app` | Compilation passed; it is not device-runtime evidence. |
| MySQL/Flyway smoke | Showcase MySQL 8.4 + Redis + Java | Flyway V1–V9 applied; live status reported V9 and readiness returned HTTP 200. |
| Showcase verification | `scripts/showcase/verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke` | Exit 0; `SHOWCASE_VERIFY_OK`, order replay/conflict, inventory evidence and 5-then-429 rate-limit smoke passed. |
| Browser walkthrough | Admin + Mobile H5 at `127.0.0.1`, mobile viewport `390×844` | Product/SKU, cart, checkout, order result/detail, AI, Evidence and Trace pages loaded from real local APIs; no horizontal overflow or broken images observed. |
| CI workflow structure | Python YAML parse and local inspection | Parsed successfully; five intended jobs present. |

The mobile build printed existing UniApp update and dynamic-import advisories. The H5 browser session also printed an existing `uni-stat 2.0` injection warning. They did not fail the build and do not establish non-H5 runtime support. A transient Admin Vite process exit was handled by restarting the controlled Showcase lifecycle; the final status and verification passed.
