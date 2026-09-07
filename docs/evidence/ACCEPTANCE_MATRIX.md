# CommerceFlow Acceptance Matrix

This is the current implementation matrix for the local CommerceFlow Showcase. `PASS` means the behavior exists in the repository and has a corresponding automated or live local verification. It does not mean production readiness.

## P0 — completion blockers

| Function | Status | Implementation location | Verification | Demo / resume impact |
| --- | --- | --- | --- | --- |
| Java API startup and MySQL initialization | PASS | `scripts/showcase/start.ps1`, `docker-compose.yml`, `apps/mall-api/src/main/resources/db/migration/` | Showcase start plus readiness check; Flyway V1–V10 applied | Makes the project reproducible and runnable |
| Redis configuration and health boundary | PASS | `.env.example`, `docker-compose.yml`, `apps/mall-api/src/main/java/com/commerceflow/mall/ai/ratelimit/` | Java Redis integration tests; Showcase status and AI response headers | Supports a truthful local rate-limit claim |
| Product / SKU / cart / order APIs use real data | PASS | `catalog/`, `cart/`, `order/` Java modules and both web clients | Java API tests; Admin and H5 browser walkthrough | Core commerce path is demonstrable |
| Required idempotency key | PASS | `OrderController.java`, `OrderService.java`, `V1__commerceflow_schema.sql` | Missing, oversized, replay, in-progress/conflict tests | Reliable order submission claim |
| Non-negative inventory and shortage rollback | PASS | `OrderService.java`, `OrderRepository.java`, `V1__commerceflow_schema.sql` | Shortage, partial-deduction rollback, and 12-request concurrency tests | Main transaction-reliability claim |
| Duplicate SKU strategy | PASS | `OrderService.java` aggregation and deterministic SKU ordering | `OrderFlowTests.duplicateSkuLinesAreAggregatedIntoOneItemMovementAndImageSnapshot` | Explains lock and item semantics |
| Parent Product sale-state enforcement | PASS | `CatalogRepository.java`, `CartRepository.java` | Catalog, cart, and order off-sale regression tests | Prevents stale/off-sale purchase paths |
| Nested order validation | PASS | `ApiModels.OrderRequest` and `OrderLineRequest` | Zero quantity MockMvc regression test | Prevents invalid quantities reaching writes |
| Single-currency contract | PASS | `ApiModels.SUPPORTED_CURRENCY`, `OrderService.java`, `V9__enforce_cny_and_positive_order_quantities.sql` | V9 Flyway migration and Java migration-count tests | README/API/database are explicit: CNY only |
| Backend E0 API contract freeze | PASS | `CommerceApiContract.java`, controller mappings, `/api` compatibility contract | `CommerceApiContractTest`, full Java regression | Current routes, CNY, `CREATED`, and key order/AI response shapes are protected; E1-E4 are not implied |
| Phase 0-B interface boundary and authorization closure | PASS / EXTERNAL-CONTRACT | `/api/v1/me`, `/api/v1/operator`, `CurrentUserPort`, `OperatorScope`, runtime profiles, frontend API wrappers | `Phase0BBoundaryApiTests`, `ScopedApiContractTest`, `ShowcaseRuntimePolicyTests`, Admin/Mobile tests and builds | Consumer requests omit client `userId`; cross-user reads require Operator; local Demo is explicit; staging/production select OIDC and fail closed; real IdP token acceptance remains pending |
| Backend E1 server-derived user scope | PASS / EXTERNAL-CONTRACT | `CurrentUserPort`, `ShowcaseCurrentUserAdapter`, `ExternalIdentityResolver`, `/api/v1/me` controllers | `ScopedApiContractTest`, `CurrentUserScopeTests`, external identity unit coverage, full Java regression | Versioned cart/order/AI routes derive identity from a verified JWT and mapped `user_account`; no real IdP token has been accepted in this workspace |
| Backend E2 order ports and transactional outbox | PASS / FOUNDATION | `order/application/port/out/`, `inventory/application/port/out/`, `outbox/`, `V10__add_transactional_outbox.sql` | Port boundary tests, Outbox integration tests, full Java regression | Order claims idempotency before inventory reservation; `ORDER_CREATED` remains pending in the same transaction; worker/broker is not implemented |
| Backend E3 Provider boundary and observability | PASS / LOCAL-CONTRACT | `ai/application/port/out/CustomerServiceProvider.java`, Provider adapters, `AiProviderMetrics.java`, `application.yml` | Local OpenAI-compatible HTTP fixture, Spring external-mode context, error/retry/metrics tests, Java regression | GPT/DeepSeek-compatible server-side adapter is ready for explicit runtime configuration; no real key, model-quality, billing or public-runtime claim |
| Backend E4 staging deployment assets | PASS / NOT_LIVE | `deploy/staging/`, `scripts/staging/`, `application-staging.yml`, Dockerfiles and reverse-proxy templates | `docker compose config`, TLS-profile config inspection, placeholder/secret environment validation; image build awaits DockerHub connectivity | Reproducible staging skeleton is reviewable; no server, DNS, certificate, backup/restore or public deployment claim |

## P1 — quality and portfolio value

| Function | Status | Implementation location | Verification | Demo / resume impact |
| --- | --- | --- | --- | --- |
| Order snapshots and inventory movement evidence | PASS | `order/`, `V5`–`V7` migrations, MyBatis mapper | Java tests and Admin order evidence page | Makes transaction behavior inspectable |
| Redis Lua fixed-window limiter | PASS | `ai/ratelimit/redis/ai-rate-limit.lua`, limiter classes | Five real Redis integration tests plus unit tests; optional live 429 smoke | Supports 5 / 60s and `Retry-After` claim |
| AI facts, Evidence, Trace, and Java fallback | PASS / MOCK | `AiService.java`, `AiTraceRepository.java`, Provider adapters, FastAPI `providers.py` | Python 11 tests, Java AI/provider tests, live Admin/H5 AI response | Strong AI application story with explicit Mock boundary; external adapter contract is tested locally |
| Admin operations, catalog, order evidence, and AI workbench | PASS | `apps/admin-web/src/` | Admin tests/build and local browser walkthrough | Portfolio-grade real API review surface |
| Persistent H5 cart, checkout, order result/detail, and AI page | PASS / H5_VERIFIED | `apps/mobile-app/src/pages/` | Mobile tests/builds and 390×844 browser walkthrough | Demonstrates the vertical commerce flow |
| Unified error states and retries | PASS | Admin workbench components and mobile page states | Frontend test suites and visible error/loading/empty branches | Avoids fake-success UI |
| Health/readiness and local lifecycle scripts | PASS | `scripts/showcase/`, `application.yml` | Start/status/verify/stop lifecycle and readiness endpoints | Reproducible local delivery |
| CI quality gates | PASS | `.github/workflows/ci.yml` | YAML inspected locally; workflow covers integrity, Java/MySQL/Redis, Python, Admin, Mobile | Shows engineering discipline; CI does not call a remote AI |

## P2 — presentation and documentation

| Function | Status | Implementation location | Verification | Demo / resume impact |
| --- | --- | --- | --- | --- |
| Chinese-first portfolio presentation | PASS | `README.md`, Admin and H5 pages, `screenshots/v2/` | Canonical screenshot dimensions and local browser captures | Suitable for a 3–5 minute walkthrough |
| CNY / Mock / demo-user / external-provider boundary disclosure | PASS | `README.md`, `docs/design/BACKEND_E3_PROVIDER_OBSERVABILITY.md`, `docs/release/P7_FEATURE_BOUNDARY_MATRIX.md` | Repository integrity scan and manual review | Keeps resume language truthful |
| Resume fact classification and ownership disclosure | PASS | `docs/career/RESUME_FACTS.md`, `docs/career/OWNERSHIP_GAPS.md` | Manual review against current code and test evidence | Prevents production/authorship overclaiming |

## Explicitly not accepted as completed

Payment, logistics, refunds, address management, production authentication acceptance, real customer data, real remote model/API-key acceptance, production deployment, throughput or scale claims, native Android/iOS runtime, and non-H5 device runtime remain out of scope. The local AI provider is deterministic `commerceflow-mock`; the Java OpenAI-compatible adapter is contract-tested and wired for runtime configuration but has not been accepted against a paid external model; Java fallback is implemented but is not a real model.
