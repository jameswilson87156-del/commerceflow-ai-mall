# Real Feature Matrix

This matrix is the implementation baseline for future design references. It intentionally records missing interfaces instead of filling them with placeholder data.

## Runtime Feature Matrix

| Capability | Current status | Real API or source | Reliable fields | Current design implication |
| --- | --- | --- | --- | --- |
| Demo login | Implemented | `POST /api/auth/demo-login` | `userId`, `username`, `displayName` | May show `Demo Buyer`; do not show auth tokens, roles, or account settings |
| Product list | Implemented | `GET /api/products` | `id`, `name`, `description`, `categoryName`, nested `skus` | Real catalog cards and table rows are allowed |
| Product detail | Implemented in API | `GET /api/products/{productId}` | Same product and SKU fields | Frontends can show a real detail view; current UniApp uses the list response instead |
| SKU | Implemented as nested read data | Product APIs | `id`, `skuCode`, `color`, `size`, `salePrice`, `currency`, `availableStock` | Real variant selection and stock label are allowed |
| Inventory deduction | Implemented in order transaction | Java `OrderService` and `inventory` table | `availableStock` before/after only if separately queried; zero affected rows means insufficient or changed stock | Show outcome, not an unqueried audit timeline |
| Cart read/add | Implemented in Java | `GET /api/cart?userId=1`, `POST /api/cart/items?userId=1` | Cart item ID, SKU, product name, SKU code, attributes, unit price, quantity, available stock | API supports a persistent cart view; current UniApp does not use it |
| Order submit | Implemented | `POST /api/orders?userId=1` with `Idempotency-Key` | `orderNo`, amount, currency, `CREATED`, timestamps, order item snapshots | Success screen can show the real order number and amount |
| Order list/detail | Implemented in Java | `GET /api/orders?userId=1`, `GET /api/orders/{orderNo}` | order number, amount, currency, status, created time, product/SKU/attribute/price/quantity snapshots | Vue order table is real; a detailed panel still needs frontend work |
| Idempotency | Implemented and tested | HTTP Header `Idempotency-Key` plus MySQL unique constraint | Replay result, in-progress conflict, key-reuse conflict | Can be explained in an order detail or interview evidence panel; not as a visible user KPI |
| AI product support | Implemented backend; Vue workbench deferred to P4C | `POST /api/ai/customer-service/ask` | selected product/SKU, typed answer status, provider, Java Evidence, safe facts, compact Trace | A future panel must select a real SKU and render plain text only |
| Java-owned business facts | Implemented internally | Java to Python `POST /internal/ai/customer-service/answer` | question, product/SKU identity/status/image, `unitPrice` decimal string, currency, stock, snippets, queried time | Java is the sole fact source; Python cannot add Evidence or query MySQL |
| AI provider fallback | Implemented | Bounded Java call plus Java fact fallback | `FALLBACK_ANSWER`, `java-fact-fallback`, warning, safe error code | State can be shown in a future support response, not as a fake reliability percentage |
| AI product copy | Not implemented | None | None | Design-only future reference; no runtime screenshot |
| Trace detail | Write-only for now | extended `ai_trace` table | correlation ids, fact selection ids, safe question category, answer/provider/fallback/latency/error summary | Future page; still requires a dedicated read API and DTO |
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
| Vue Overview | KPI cards, catalog signal, recent orders, AI signal | Products and orders are live; stock is computed locally | No aggregate metrics, trend API, latest activity, or trace read API |
| Vue Catalog | Read-only table | Product API is live | No filters, pagination, write actions, or SKU detail panel |
| Vue Orders | Read-only table | Order list is live | No order detail page, inventory deduction evidence, or status transitions |
| Vue AI Support | Sidebar label only | P4B backend is live but no Vue request surface yet | P4C must add real SKU selection, messages, loading/error/retry, Evidence, and Trace rendering |
| Vue AI Copy Desk | Label and prompt shell | It currently reuses product chat, which is not copy generation | Requires a dedicated backend contract before it may appear in a runtime screenshot |
| Vue Trace Explorer | Label and prompt shell | No trace read call | Requires trace detail endpoint and evidence renderer |
| UniApp Mall | Catalog list | Product list is live | Demo login call and product detail endpoint are not used by the page |
| UniApp Product sheet | Local sheet and SKU add | SKU values originate from product list | Needs a real route or dedicated detail state for a stable reference image |
| UniApp Bag | Local array | Order submit is live; cart API is not used | Needs persisted cart read, confirmation state, success screen, and order navigation |
| UniApp Orders | Bottom label only | No call | Requires order list/detail view before a runtime screenshot can claim it |

## API Gap Register

The following interfaces are design prerequisites, not tasks to silently assume as complete:

1. A dashboard summary endpoint with explicit time zone and date-window semantics.
2. An order aggregation endpoint if a trend chart or pending-work KPI is desired.
3. A trace detail endpoint that reads `ai_trace` and returns parsed evidence, risk, and business facts without exposing unrelated privacy data.
4. A dedicated AI copy endpoint and DTO if AI Copy Desk becomes an actual feature.
5. A persistent frontend cart/order-success flow if UniApp is expected to demonstrate a multi-screen order journey.
6. Product/SKU write APIs only if the admin design is changed from read-only catalog inspection to management.

Until an item has an implemented API, it stays `FUTURE_SCOPE` or `REMOVE` in screenshot plans.
