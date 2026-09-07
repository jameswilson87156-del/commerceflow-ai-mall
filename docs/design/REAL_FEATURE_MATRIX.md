# Real Feature Matrix

This matrix is the implementation baseline for future design references. It intentionally records missing interfaces instead of filling them with placeholder data.

## Runtime Feature Matrix

| Capability | Current status | Real API or source | Reliable fields | Current design implication |
| --- | --- | --- | --- | --- |
| Demo login | Implemented | `POST /api/auth/demo-login` | `userId`, `username`, `displayName` | May show `Demo Buyer`; do not show auth tokens, roles, or account settings |
| Product list | Implemented | `GET /api/products` | `id`, `name`, `description`, `categoryName`, nested `skus` | Real catalog cards and table rows are allowed |
| Product detail | Implemented and used by H5 | `GET /api/products/{productId}` | Same product and SKU fields | UniApp detail page reloads the authoritative detail endpoint and selects a real SKU |
| SKU | Implemented as nested read data | Product APIs | `id`, `skuCode`, `color`, `size`, `salePrice`, `currency`, `availableStock` | Real variant selection and stock label are allowed |
| Inventory deduction | Implemented in order transaction | Java `OrderService` and `inventory` table | `availableStock` before/after only if separately queried; zero affected rows means insufficient or changed stock | Show outcome, not an unqueried audit timeline |
| Cart read/add | Implemented and used by H5 | Consumer `GET/POST/PUT/DELETE /api/v1/me/cart...` via `CurrentUserPort` | Cart item ID, SKU, product name, SKU code, attributes, unit price, quantity, available stock | H5 reads and mutates the persistent server-scoped cart; no client `userId` |
| Order submit | Implemented | Consumer `POST /api/v1/me/orders` with `Idempotency-Key` | `orderNo`, amount, currency, `CREATED`, timestamps, order item snapshots | Success screen can show the real order number and amount for the current consumer scope |
| Order list/detail | Implemented and used by both clients | Consumer `GET /api/v1/me/orders...`; Operator `GET /api/v1/operator/orders...` | order number, amount, currency, status, created time, product/SKU/attribute/price/quantity snapshots | Admin evidence page uses OperatorScope; H5 result/detail uses CurrentUserPort |
| Idempotency | Implemented and tested | HTTP Header `Idempotency-Key` plus MySQL unique constraint | Replay result, in-progress conflict, key-reuse conflict | Can be explained in an order detail or interview evidence panel; not as a visible user KPI |
| AI product support | Implemented in Java, FastAPI, Vue, and UniApp H5 | Consumer `POST /api/v1/me/ai/customer-service/ask`; Operator `POST /api/v1/operator/ai/customer-service/ask` | selected product/SKU, typed answer status, provider, Java Evidence, safe facts, compact Trace | Both frontends select a real SKU and render provider, Evidence, Trace, loading, error, and rate-limit states; identity scope is server-derived |
| Java-owned business facts | Implemented internally | Java to Python `POST /internal/ai/customer-service/answer` | question, product/SKU identity/status/image, `unitPrice` decimal string, currency, stock, snippets, queried time | Java is the sole fact source; Python cannot add Evidence or query MySQL |
| AI provider fallback | Implemented | Bounded Java call plus Java fact fallback | `FALLBACK_ANSWER`, `java-fact-fallback`, warning, safe error code | Current Admin and H5 support pages show the fallback state; no reliability percentage is claimed |
| AI product copy | Not implemented | None | None | Design-only future reference; no runtime screenshot |
| Trace detail | Current-response read model implemented; history out of scope | response from the versioned consumer/Operator AI endpoints plus extended `ai_trace` table | correlation ids, fact selection ids, safe question category, answer/provider/fallback/latency/error summary | Admin and H5 render the returned current Trace; no history/search claim |
| AI history | Not implemented | None | None | Do not show conversation list or completion rate |
| Product/SKU management writes | Not implemented | None | None | Current Catalog view is read-only, not an admin CRUD claim |
| Categories | Read-only through product response | `categoryName` nested in products | category name | Do not show a category management screen |
| Payment | Not implemented | None | None | Remove payment badges, payment status, and settlement KPIs |
| Logistics | Not implemented | None | None | Remove shipment, delivery, carrier, and tracking UI |
| Marketing | Not implemented | None | None | Remove campaign and conversion panels |
| User management | Not implemented | Demo user row only | one demo user | Remove user list and role controls |

## Frontend Surface Matrix

| Surface | Existing UI | API-backed today | Gap to call it a complete design reference |
| --- | --- | --- | --- |
| Vue Overview | KPI cards, catalog signal, recent orders, AI signal | Operations overview API is live and backed by MySQL/`ai_trace`/runtime configuration | No date-window trend API or trace-history search |
| Vue Catalog | Read-only search/filter/detail/SKU view | Product API is live | No pagination or write actions; this is not CRUD management |
| Vue Orders | Read-only list plus evidence detail | Order list/detail/evidence APIs are live | No status transitions or fulfillment operations; those remain out of scope |
| Vue AI Support | Real SKU selection and single-round workbench | Customer-service API is live | No AI history/search or remote-provider acceptance |
| Vue AI Copy Desk | Label and prompt shell | It currently reuses product chat, which is not copy generation | Requires a dedicated backend contract before it may appear in a runtime screenshot |
| Vue Trace Explorer | Label and prompt shell | No trace read call | Requires trace detail endpoint and evidence renderer |
| UniApp Mall | Catalog list | Product list is live | Catalog uses the configured `Local Demo Fixture` or `Real Backend` label; any consumer mutation still requires `/api/v1/me` identity |
| UniApp Product sheet | API-backed detail and SKU add | Product detail, SKU and cart APIs are live | H5 route verified; native runtime remains compile-only |
| UniApp Bag | Persistent server cart | Cart read/add/update/delete APIs are live | H5 checkout and order result are verified; payment is out of scope |
| UniApp Orders | Real order list/detail/result pages | Order list/detail APIs are live | H5 order result/detail verified; native runtime remains compile-only |

## API Gap Register

The following interfaces are design prerequisites, not tasks to silently assume as complete:

1. A dashboard summary endpoint with explicit time zone and date-window semantics.
2. An order aggregation endpoint if a trend chart or pending-work KPI is desired.
3. A trace detail endpoint that reads `ai_trace` and returns parsed evidence, risk, and business facts without exposing unrelated privacy data.
4. A dedicated AI copy endpoint and DTO if AI Copy Desk becomes an actual feature.
5. Native device runtime acceptance if UniApp is expected to make an Android/iOS/mini-program runtime claim.
6. Product/SKU write APIs only if the admin design is changed from read-only catalog inspection to management.

Until an item has an implemented API, it stays `FUTURE_SCOPE` or `REMOVE` in screenshot plans.
