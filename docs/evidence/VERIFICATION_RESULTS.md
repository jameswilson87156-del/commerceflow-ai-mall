# Showcase V1 Verification Results

Verified locally on 2026-07-25. The commands below were run against the independent repository at `D:/workhome/commerceflow-ai-mall`.

| Area | Command or check | Result |
| --- | --- | --- |
| MySQL | Docker Compose healthcheck | healthy on local port 3307 |
| Java | `cmd /c ..\\..\\mvnw.cmd -f pom.xml test` from `apps/mall-api` | 4 tests passed |
| Python | `py -3 -m pytest tests` from `services/ai-service` | 2 tests passed |
| Vue admin | `npm run build` from `apps/admin-web` | passed |
| UniApp H5 | `npm run build` from `apps/mobile-app` | passed |
| UniApp compiler | `npm run build:uni` from `apps/mobile-app` | passed |
| Java to AI | `POST /api/ai/product-chat` with Mock Provider | `COMPLETED`, `java.businessFacts` evidence returned |
| Order idempotency | submit and replay with one `Idempotency-Key` | same order number returned |
| Key misuse | reuse key with a different request body | HTTP 409 |
| Screenshots | eight local runtime captures | generated under `screenshots/` |

The Python test run emitted one Starlette/httpx deprecation warning. It did not fail the test suite and is retained as a follow-up maintenance item rather than hidden.

## Scope Boundary

The showcase is a local demonstration environment. It does not claim production payment, logistics, public deployment, or independent from-scratch authorship. Ownership gaps remain tracked in `docs/career/OWNERSHIP_GAPS.md`.
