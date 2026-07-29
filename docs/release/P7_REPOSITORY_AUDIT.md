# P7 Repository Audit

## Baseline

- Audit branch: `feat/p7-showcase-release-audit`.
- Frozen source baseline: `feat/p6-mobile-final-freeze` at `17c13ba2b2b581c23d3c49940d767f6bf4ac35cf`.
- This audit changes documentation only. P2-P6 code, Flyway V1-V8, Redis Lua, evidence, and frozen screenshots remain untouched.

## Structure And Responsibility

| Path | Responsibility | Audit result |
| --- | --- | --- |
| `apps/mall-api` | Java 17 Spring Boot modular monolith, MySQL/Flyway, OpenAPI, order transaction, AI gateway, Redis limiter | Active source of backend facts. |
| `services/ai-service` | FastAPI structured-answer provider | Active local Mock provider; no database access. |
| `apps/admin-web` | Vue 3 admin showcase | Product/SKU, order evidence, and AI workbench are active. |
| `apps/mobile-app` | UniApp mobile showcase | H5 is accepted runtime; non-H5 is compile-only. |
| `infra`, `docker-compose.yml` | Local MySQL and Redis runtime | Compose health checks exist; persistent data remains ignored. |
| `scripts` | Local helper commands | Useful but not one coordinated lifecycle. |
| `docs/evidence` | Phase verification records | P2-P6 evidence exists; freeze records identify final screenshots. |
| `screenshots/v2` | Real local runtime evidence | Multiple historical candidates exist; P7C must select canonical files. |

## Verified Frozen Evidence

| Phase | Freeze record | Local verification recorded |
| --- | --- | --- |
| P3 | `docs/evidence/P3-order-inventory/P3_FINAL_FREEZE.md` | Java 14; Vue 13; Flyway V1-V7; final 1920x1080 order screenshot. |
| P4 | `docs/evidence/P4-ai-customer-service/P4_FINAL_VISUAL_FREEZE.md` | Java 28; Python 11; Vue 35; empty-MySQL migration and two 1920x1080 captures. |
| P5 | `docs/evidence/P5-ai-rate-limit/P5_FINAL_FREEZE.md` | Java 38 including 5 Redis integration tests; Python 11; Vue 43; normal, 429, and FAIL_OPEN captures. |
| P6 | `docs/evidence/P6-mobile-commerce/P6_FINAL_FREEZE.md` | Java 43; Python 11; admin Vue 43; mobile 31; H5 build and non-H5 compile. |

## Repository Health

- The P7A start worktree was clean and `HEAD...origin/feat/p6-mobile-final-freeze` was `0/0`.
- No unignored files, no tracked file above 50 MB, and no high-confidence key pattern were found in the audit scan.
- Ignored local build directories exist (`node_modules`, `dist`, `.venv`, caches). They are not tracked and must stay that way.
- Root has no `main` branch locally or remotely. The remote default `HEAD` still points to the historic P3 branch. This is a release-governance gap, not a reason to alter it in P7A.
- `screenshots/v2` contains historical and final variants. Retain them as evidence, but do not present all of them as equal README material.
- Existing source-provenance records state that reviewed ecommerce repositories were comparison-only and no third-party source code, data, UI, or assets were copied. P7D must repeat a history-aware check before any public visibility change.

## Findings

| Priority | Finding | Release disposition |
| --- | --- | --- |
| P0 | No reproducible frozen-code, migration, or secret-exposure blocker was found in this documentation audit. | No P0 code repair is scheduled by P7A. |
| P1 | Root README and first-run guidance do not yet describe the full P2-P6 runtime, Redis rate-limit boundary, mobile H5 acceptance, or canonical evidence. | P7C. |
| P1 | CI builds modules but does not provide MySQL/Flyway runtime coverage, front-end tests, mobile tests, secret scan, or screenshot/link checks. | P7C. |
| P1 | There is no coordinated Windows PowerShell start/status/stop lifecycle; default CORS ports omit the H5 port used in prior local capture unless overridden. | P7B. |
| P1 | The visible admin `运营总览` entry is not an active data-backed view. It must not be used as a KPI dashboard or canonical screenshot now. | P7B design and, only if approved, minimum read-only implementation. |
| P2 | JDBC write/read repositories coexist with a focused MyBatis read-only evidence mapper. This is intentional today but needs boundary documentation. | Accepted for Showcase V1; document in architecture. |
| P2 | Demo user id defaults appear in controllers and front-end runtime configuration. | Consolidate only when changing runtime configuration in a later approved phase. |
| Accepted boundary | Mock AI, local Docker data, demo identity, and H5 acceptance are showcase constraints, not production claims. | Preserve in README, portfolio, and interview material. |

## Audit Scope Limits

This was a source and metadata audit, not a penetration test, license legal opinion, production load test, or a re-run of every frozen phase. Freeze records are cited as verification evidence; P7D must re-run the final release matrix from an empty local runtime.
