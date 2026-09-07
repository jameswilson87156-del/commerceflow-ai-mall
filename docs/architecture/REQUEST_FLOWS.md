# Request Flows

| Flow | Entry | Authoritative storage | Detail |
| --- | --- | --- | --- |
| Catalog | Admin or UniApp -> `GET /api/products` | MySQL | Product, SKU, image path and available stock are returned by Java. |
| Cart | UniApp -> `/api/v1/me/cart` | MySQL | `CurrentUserPort` supplies the consumer scope; the client does not supply `userId`. |
| Order | Client -> `POST /api/v1/me/orders` | MySQL | Consumer scope is server-derived; idempotency, stock deduction, snapshots and movement evidence are transactional. |
| AI customer service | Consumer -> `POST /api/v1/me/ai/customer-service/ask`; Operator -> `POST /api/v1/operator/ai/customer-service/ask` | MySQL + FastAPI + Redis | Java owns facts and trace persistence; Redis protects both explicit identity scopes. |
| Operations overview | Operator -> `GET /api/v1/operator/operations/overview` | MySQL / `ai_trace` / configuration | `OperatorAuthorizationPolicy` protects the read-only aggregate; no frontend role parameter is trusted. |

Use the focused documents for the order, AI, and Redis step-by-step sequences.
