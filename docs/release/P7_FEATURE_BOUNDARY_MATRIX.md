# P7 Feature Boundary Matrix

Status meanings: `IMPLEMENTED` is present in source; `VERIFIED` has frozen local evidence; `MOCK` is intentionally deterministic/local; `COMPILE_ONLY` is not device-runtime evidence; `OUT_OF_SCOPE` must not be claimed.

| Area | Status | Current fact | Evidence / boundary |
| --- | --- | --- | --- |
| Product, category, SKU, available stock | IMPLEMENTED, VERIFIED | Java product APIs and local MySQL demo catalog feed both web clients. | P2/P6 freeze records. |
| Product/SKU images | IMPLEMENTED, VERIFIED | Local original assets are API-backed paths and are copied into app runtime assets. | P2, P3, P6 evidence. |
| Cart | IMPLEMENTED, VERIFIED | Demo-user cart add/list flow exists. | P6 H5 evidence. |
| Order creation | IMPLEMENTED, VERIFIED | `@Transactional` order creation uses BigDecimal, item snapshots, and conditional stock decrement. | P3/P6 evidence. |
| Idempotency | IMPLEMENTED, VERIFIED | `Idempotency-Key` plus MySQL uniqueness/fingerprint path returns original order or conflict. | P3 evidence. |
| Inventory movement evidence | IMPLEMENTED, VERIFIED | Successful order writes one stored `ORDER_DEDUCT` movement per order/SKU in the transaction. | P3 evidence. |
| Payment, logistics, shipment, refund | OUT_OF_SCOPE | Only `CREATED` exists. | Never show as implemented. |
| Admin product/SKU page | IMPLEMENTED, VERIFIED | Read-only search/filter/detail/SKU inventory presentation. | P2 final evidence. |
| Admin order evidence page | IMPLEMENTED, VERIFIED | Read-only order, snapshots, idempotency, and movement proof. | P3 final evidence. |
| Admin operations overview | IMPLEMENTED, VERIFIED | Read-only MyBatis aggregate of local product/SKU/inventory/order/`ai_trace` facts and runtime boundaries. | P7B evidence and canonical overview screenshot. |
| AI customer service | IMPLEMENTED, VERIFIED, MOCK | Java owns facts, calls FastAPI Mock, validates response, persists Java evidence/trace. | P4/P6 evidence. |
| Remote OpenAI-compatible provider | OUT_OF_SCOPE | Placeholder/config boundary only; no real key or external acceptance. | Do not advertise as active. |
| Java fact fallback | IMPLEMENTED, VERIFIED | Provider failure or invalid response returns a fact-bound Java fallback. | P4/P5/P6 evidence. |
| Trace history/search | OUT_OF_SCOPE | Current answer returns its trace; no history exploration claim. | Do not add a fake console. |
| Redis AI rate limit | IMPLEMENTED, VERIFIED | Redis 8.0.2 Lua fixed-window limiter, HTTP 429, Retry-After and documented FAIL_OPEN. | P5 freeze. |
| Redis for orders/inventory/cache/session | OUT_OF_SCOPE | Redis only protects AI request rate. | Explicit P5/P6 boundary. |
| UniApp H5 commerce and AI | IMPLEMENTED, VERIFIED | Local H5 product/cart/order/AI path is accepted. | P6 freeze. |
| Native mobile / mini-program runtime | COMPILE_ONLY | UniApp non-H5 compilation passed. | No device/runtime claim. |
| Authentication | MOCK | Demo login and demo user id only. | Not production auth. |
| CI | IMPLEMENTED | GitHub Actions defines repository integrity, Java/MySQL/Redis, Python, Admin and Mobile jobs. | P7C workflow and local validation; remote P7C run awaits a later push. |
