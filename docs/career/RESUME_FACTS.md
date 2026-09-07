# Resume Facts

Use this list as a fact check before writing a resume or portfolio entry. It reflects the local Showcase implementation and the verification run on 2026-09-02; it is not a production metrics sheet.

## Implemented and tested

- Java 17 / Spring Boot 3 modular monolith with Product, SKU, Inventory, Cart, Order, AI customer-service, and read-only operations modules.
- MySQL-backed `@Transactional` order flow using `BigDecimal`, CNY-only money semantics, repeated-SKU aggregation, deterministic SKU lock order, conditional non-negative stock deduction, order-item snapshots, inventory movement evidence, cart cleanup, and rollback behavior.
- `Idempotency-Key` enforcement with a 120-character boundary, request fingerprint, application in-flight guard, and MySQL `UNIQUE(user_id, idempotency_key)` protection.
- Flyway V1–V10 schema lifecycle, including CNY/positive-quantity constraints and a transactional `outbox_event` foundation for `ORDER_CREATED`.
- Redis Lua fixed-window AI rate limiter with 5 requests per 60 seconds, HTTP 429, `Retry-After`, and a documented local `FAIL_OPEN` behavior.
- Java-owned Product/SKU/Inventory facts passed to FastAPI, seven Java Evidence records, six Trace steps, bounded trace persistence, and fact-bound Java fallback behavior.
- Provider SPI with a default FastAPI adapter and an OpenAI-compatible adapter that can target OpenAI/DeepSeek by environment configuration; local fixture tests cover response parsing, auth-header handling, retry/error classification, and no-secret request-body leakage. No real provider credential or model-quality claim is included.
- Low-cardinality Micrometer Provider request/latency metrics with fixed mode/outcome tags; no raw prompt, answer, Trace ID or key is used as a metric dimension.
- Vue 3 + TypeScript Admin pages for live operations overview, read-only Product/SKU inventory inspection, order execution evidence, and AI customer service.
- UniApp H5 product detail, server-backed cart, checkout, `CREATED` order result/detail, and AI customer service flow.

## Implemented but dependent on local services

- The Showcase runtime depends on local Docker MySQL 8.4 and Redis 8.0.2 containers, plus local Java and FastAPI processes. The lifecycle script starts and stops only the processes it owns.
- The browser walkthrough uses local synthetic demo data and `userId=1`; it is not an authenticated user session.

## Mock or fallback only

- `commerceflow-mock` is the default AI provider. It is deterministic, receives Java-supplied facts, and does not call an external model.
- `java-fact-fallback` handles provider timeout, unavailability, provider errors, or invalid structured responses. It is rule-based, not model quality evidence.
- Demo login and `userId=1` are a showcase identity boundary, not production authentication or authorization.

## Page or compile-only evidence

- Admin and H5 pages are locally API-backed and browser-verified.
- Non-H5 UniApp targets are compile-only evidence. No Android, iOS, or mini-program device runtime claim is made.
- Historical screenshots under `screenshots/v2/` are local runtime evidence; they are not production screenshots or independent authorship evidence.

## Planned or out of scope

- Payment, shipment/logistics, refunds, addresses, coupons, real registration/login, roles/permissions, public deployment, real remote-provider/API-key acceptance, production alerting/observability, and throughput/scale metrics.
- AI trace history/search and product/SKU write APIs are not claimed beyond the implemented current-response and read-only views.

Ownership remains explicitly documented as `NOT_PASSED` in [OWNERSHIP_GAPS](OWNERSHIP_GAPS.md); the project should not be described as independently written from scratch until the learning rebuild is completed.
