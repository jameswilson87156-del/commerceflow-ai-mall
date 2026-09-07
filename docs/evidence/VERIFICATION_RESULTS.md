# CommerceFlow Local Verification Results

Verified locally on 2026-09-02 from the independent repository at `D:/workhome/commerceflow-ai-mall`. This record reports local execution only; it is not production, throughput, model-quality, or device-runtime evidence.

## Automated checks

| Area | Command | Exit code | Result |
| --- | --- | ---: | --- |
| Repository integrity | `py -3 scripts/ci/verify_repository.py` | 0 | `REPOSITORY_INTEGRITY_OK`; README/architecture links, tracked-file policy, canonical image dimensions, obvious-secret patterns and local-path checks passed |
| Java + H2 + Redis integration | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 0 | 53 tests passed, 0 failures, 0 errors, 0 skipped; includes 5 real Redis integration tests against local Redis 8.0.2 |
| Python AI service | `py -3 -m pytest` from `services/ai-service` | 0 | 11 tests passed |
| Admin unit/component tests | `npm test` from `apps/admin-web` | 0 | 45 tests passed across 6 test files |
| Admin production build | `npm run build` from `apps/admin-web` | 0 | Type-check and Vite build passed |
| Mobile policy tests | `npm test` from `apps/mobile-app` | 0 | 2026-09-02 baseline 31 tests passed；2026-09-03 EC-UI-02 当前运行 35 tests passed |
| Mobile H5 build | `npm run build` from `apps/mobile-app` | 0 | Passed |
| Mobile non-H5 compiler | `npm run build:uni` from `apps/mobile-app` | 0 | Compilation passed; this is not native device runtime evidence |

## Runtime checks

| Area | Command or action | Result |
| --- | --- | --- |
| Showcase lifecycle | `$env:MALL_API_PORT='8081'; powershell -NoProfile -File .\scripts\showcase\start.ps1 -IncludeMobile -NoBrowser -ForceRestart -WaitTimeoutSeconds 90` | Started local MySQL, Redis, Java, FastAPI, Admin and Mobile H5; the selected API port was 8081 because 8080 had a foreign listener |
| Readiness and migration | `powershell -NoProfile -File .\scripts\showcase\status.ps1` | API/readiness/Python/Admin/Mobile all HTTP 200; MySQL and Redis healthy; Flyway `V9`; provider `commerceflow-mock / MOCK` |
| Baseline Showcase verification | `powershell -NoProfile -File .\scripts\showcase\verify.ps1 -IncludeMobile` | Exit 0, `SHOWCASE_VERIFY_OK`; real product/cart/order/overview/AI/provider/Evidence/Trace/header/image checks passed |
| Order smoke | `powershell -NoProfile -File .\scripts\showcase\verify.ps1 -IncludeMobile -IncludeOrderSmoke -IncludeRateLimitSmoke` | Exit 0; `ORDER_SMOKE_OK` proved create, same-key replay, different-body `409`, and execution evidence |
| Redis rate-limit smoke | Same optional Showcase verification command | Exit 0; `RATE_LIMIT_SMOKE_OK` proved 5 allowed requests followed by 1 HTTP 429 with rate-limit headers |
| Admin browser walkthrough | Local Admin at `http://127.0.0.1:5174` | Real operations, Product/SKU, order evidence, and AI pages loaded; AI returned `commerceflow-mock`, 7 Evidence items and 6 Trace steps; no browser error/warning logs were observed on Admin |
| Mobile browser walkthrough | Local H5 at `http://127.0.0.1:5173`, viewport `390×844` | Product list/detail, server cart, checkout, `CREATED` order result/detail, and AI response all loaded; no horizontal overflow and zero broken images were observed |

## Baseline failures and their actual causes

- The first Java baseline run before Redis was started had 5 Redis integration errors caused by connection refusal at `localhost:6380`; after the local Redis container was healthy, the complete Java suite passed.
- `python -m pytest` was not usable on this Windows installation because the `python` command resolved to the Microsoft Store alias. The supported local command is `py -3 -m pytest`, which passed all 11 Python tests.
- The first optional live verification after the browser session found the Admin Vite process no longer listening on 5174. Its captured log contained only a successful Vite startup, with no compile error. The controlled lifecycle was restarted with `-ForceRestart`; the subsequent status check and full optional verification both passed.

## Warnings and boundaries

- UniApp H5 emits an existing `uni-stat 2.0` warning that `onCreateVueApp` could not inject a page-level mixin after retry. It does not block the H5 page, API calls, image loading, or builds; it remains a dependency maintenance item.
- The local order and rate-limit smoke checks intentionally write synthetic Showcase order/AI-trace data. They do not represent production volume or financial activity.
- The AI provider is deterministic `commerceflow-mock`; no external model or real API key was used. Java fallback is implemented and tested as a rule-based boundary, not model-quality evidence.
- MySQL, Redis, FastAPI and the demo identity are local Showcase dependencies. Payment, logistics, refunds, production authentication, public deployment, scale metrics, and native Android/iOS/mini-program runtime remain out of scope.

## 2026-09-03 Backend E0 contract-freeze verification

This is an additional local verification record after the 2026-09-02 baseline above. It reports the E0 implementation only; it does not upgrade the Showcase to production authentication, a real external model, payment, or public deployment.

### Implemented boundary

- Added `apps/mall-api/src/main/java/com/commerceflow/mall/api/CommerceApiContract.java` for the current `/api/...` route, Showcase identity, CNY and `CREATED` order-status contracts.
- Updated the existing controllers to use those constants without changing the public paths or HTTP methods.
- Added `apps/mall-api/src/test/java/com/commerceflow/mall/api/CommerceApiContractTest.java`, which freezes controller mappings and key `OrderSummary` / `CustomerServiceAnswer` record shapes.
- Added the detailed design record [BACKEND_E0_CONTRACT_FREEZE](../design/BACKEND_E0_CONTRACT_FREEZE.md).

### Actual command and result

```text
cd <repository-root>
.\mvnw.cmd -f apps/mall-api/pom.xml test
```

Result: `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. This includes the 2 new API contract tests and the existing Java/H2/Redis, order transaction, AI safety, CORS, health and operations tests.

The first attempt from `apps/mall-api` using `.\mvnw.cmd test` was not a code failure: the wrapper is located at the repository root. The corrected root command above is the authoritative result. The expected H2 Flyway compatibility warning and the intentional Redis-down readiness warning were observed; neither caused a test failure.

## 2026-09-03 Backend E1 server-derived user-scope verification

This verification records the E1 implementation. It proves a versioned server-owned user-scope boundary in the local Showcase; it does not prove production authentication, RBAC, token validation, or public deployment.

### Implemented boundary

- Added `apps/mall-api/src/main/java/com/commerceflow/mall/account/application/port/out/CurrentUserPort.java` and the explicit `ShowcaseCurrentUserAdapter`.
- Added `GET /api/v1/me`, versioned cart and order routes, and `POST /api/v1/me/ai/customer-service/ask`.
- The new cart/order/AI controllers derive the user ID from `CurrentUserPort`; the scoped AI request body has no `userId` field.
- Non-`DEMO_USER` runtime mode returns `503 USER_SCOPE_UNAVAILABLE` instead of pretending the Demo adapter is real authentication.
- Added [BACKEND_E1_USER_SCOPE](../design/BACKEND_E1_USER_SCOPE.md).

### Actual command and result

```text
cd <repository-root>
.\mvnw.cmd -f apps/mall-api/pom.xml test
```

Result: `Tests run: 58, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. This includes the E1 route/parameter contract test and current-user adapter tests, plus the E0 contract, order transaction, AI safety, Redis, health and operations regression suite.

The E1 implementation is Showcase-only: `SHOWCASE_AUTHENTICATION_MODE=DEMO_USER` and `SHOWCASE_DEMO_USER_ID=1` remain the only supported identity source. A real IdP, password/session/token lifecycle, RBAC and production deployment remain future work.

## 2026-09-03 Backend E2 order-reliability verification

This verification records the E2 foundation. It proves that the order application uses explicit ports and that a pending `ORDER_CREATED` event is appended in the same local database transaction. It does not prove external message delivery, broker durability, worker retry, payment, or production throughput.

### Implemented boundary

- Added `OrderWritePort`, `IdempotencyStore`, and `InventoryPort`; `OrderService` no longer depends on the concrete `OrderRepository` or performs JDBC inventory updates itself.
- Added `OutboxEventPort` and `JdbcOutboxEventAdapter`; Flyway V10 creates `outbox_event` with deterministic `event_id`, bounded JSON payload, `PENDING` status and scheduling indexes.
- Moved durable idempotency claim ahead of inventory reservation. A same-key replay returns the existing order without a second inventory movement or Outbox event.
- The existing item snapshot, inventory movement evidence, cart cleanup, CNY, `CREATED`, concurrency and rollback tests remain active.

### Actual command and result

```text
cd <repository-root>
.\mvnw.cmd -f apps/mall-api/pom.xml test
```

Result: `Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. This includes `OrderReliabilityPortTest` (including the ascending-lock-order unit case), three `OrderOutboxTests`, the request-order preservation regression, plus the E0/E1 and existing Java/H2/Redis regression suite.

The expected H2/Flyway compatibility warning and the deliberate Redis-down readiness warning remain non-failing test conditions. E2 currently leaves Outbox rows in `PENDING`; there is no worker or external consumer, so no external publication claim is made.

## 2026-09-03 Backend E3 provider and observability verification

This verification records the E3 implementation. It proves the server-side adapter contract, error classification, bounded retry behavior, and low-cardinality metrics with local fixtures. It does not prove a paid OpenAI/DeepSeek call, model quality, billing, quota, or provider SLA.

### Implemented boundary

- Added the `CustomerServiceProvider` outbound port and retained the old `CustomerServiceProviderClient` name as a source-compatible alias for existing tests/integrations.
- Kept FastAPI as the default adapter and added a conditional OpenAI-compatible adapter for OpenAI, DeepSeek, or a compatible gateway. The browser never receives the key; Java sends only the typed question and Java-owned business facts.
- Added bounded timeout/retry and safe classification for timeout, unavailable, provider error, and invalid response cases. Added Micrometer counters/timers with fixed `mode`/`outcome` tags and no prompt, response, key, or business identifier labels.
- Added local HTTP-fixture tests for auth-header handling, no-key request-body leakage, JSON parsing, 429 retry, non-retryable 401, malformed responses, and external-mode Spring wiring.

### Actual command and result

```text
cd <repository-root>
.\mvnw.cmd -f apps/mall-api/pom.xml test
```

Result: `Tests run: 75, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. The suite includes the E3 provider boundary, external-mode context, HTTP fixture, observability, AI safety, Redis, order, API contract, and legacy-policy tests.

The default local Showcase remains `commerceflow-mock / MOCK`. No external API key was read, stored, or used in this verification.

## 2026-09-03 Backend E4 staging-asset verification

This verification records deployment assets and preflight checks. It does not claim that a staging server or public domain is live.

### Implemented boundary

- Added a private-network Docker Compose topology for MySQL, Redis, FastAPI, Java API and an Nginx edge, plus an optional Caddy TLS profile.
- Added non-root Java/Python/edge Dockerfiles, staging Spring profile settings, host-based Admin/H5 routing, `/api/` proxying, health checks and a read-only staging verification script.
- Added `scripts/staging/validate-env.ps1`; it rejects placeholder passwords, example domains, Demo identity, invalid CORS origins, and incomplete external-provider settings.

### Actual command and result

```text
docker compose --env-file .\deploy\staging\.env.example -f .\deploy\staging\docker-compose.yml config
```

Result: passed. The optional TLS profile also rendered successfully. Running the example environment through `scripts/staging/validate-env.ps1 -EnvFile .\deploy\staging\.env.example` correctly rejected `MYSQL_PASSWORD=CHANGE_ME` before any startup. The rendered Nginx template, including the `/api/actuator/` to `/actuator/` readiness mapping, passed the official `nginx:1.27-alpine` `nginx -t` check with a temporary `mall-api` hosts mapping that simulated the Compose network name.

The image build was attempted but stopped before compilation because this machine could not obtain anonymous Docker Hub base-image metadata (`auth.docker.io` connection failure over IPv6). No image-build or deployment success is claimed; rerun `docker compose ... build` on a host with registry access, then run the staging smoke and backup/restore rehearsal before public exposure.
