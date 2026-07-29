# P7B Implementation

## Scope

P7B adds a read-only operations overview and a coordinated local Showcase runtime. It does not add payment, shipment, refunds, marketing, users, write actions, a new Flyway migration, or a production claim.

## Operations overview

- `GET /api/operations/overview` is backed by `OperationsOverviewMapper` and MyBatis XML aggregation queries.
- The read model returns product and SKU counts, on-sale product count, total available stock, low-stock SKU count, `CREATED` order count and amount, recent `CREATED` orders, low-stock SKUs, `ai_trace` summary, and static runtime boundary configuration.
- `OperationsOverviewService` uses `@Transactional(readOnly = true)`. It neither writes business data nor recalculates any historical event.
- Low stock is a single backend rule: `availableStock < 30`; zero is labelled out of stock. The view only renders those facts.
- The Vue overview uses the real local endpoint. Refresh and the recent-order link are the only interactions; it contains no disabled write controls.

## Runtime implementation

- `scripts/showcase/start.ps1`, `status.ps1`, `stop.ps1`, and `verify.ps1` coordinate MySQL, Redis, Java, FastAPI, Admin, and optional Mobile H5.
- Scripts store local-only state and logs under ignored `.showcase/`. Process identity requires the recorded PID, expected listening port, and command-line hint; `stop.ps1` skips mismatches.
- The default contract is loopback ports MySQL `3307`, Redis `6380`, API `8080`, Python `8000`, Admin `5174`, Mobile H5 `5173`. All are environment-overridable. This machine had an unrelated listener on `8080`; the verified run used `MALL_API_PORT=8081` without stopping that listener.
- CORS originates from `COMMERCEFLOW_CORS_ALLOWED_ORIGINS` / the two Showcase origins. It is a finite allowlist and exposes five public rate-limit headers: `Retry-After`, `X-RateLimit-Mode`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`.

## Explicit boundaries

All values are local Showcase evidence. The order amount is labelled local Showcase order amount, not GMV or business revenue. The dashboard deliberately omits trends, growth, conversion, DAU, payment, logistics, marketing, and invented KPIs.
