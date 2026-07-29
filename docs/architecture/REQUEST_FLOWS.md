# Request Flows

| Flow | Entry | Authoritative storage | Detail |
| --- | --- | --- | --- |
| Catalog | Admin or UniApp -> `GET /api/products` | MySQL | Product, SKU, image path and available stock are returned by Java. |
| Cart | UniApp -> `/api/cart` | MySQL | Demo user `userId=1`; not a real authentication session. |
| Order | Client -> `POST /api/orders` | MySQL | Idempotency, stock deduction, snapshots and movement evidence are transactional. |
| AI customer service | Client -> `POST /api/ai/customer-service/ask` | MySQL + FastAPI + Redis | Java owns facts and trace persistence; Redis protects only this entry. |
| Operations overview | Admin -> `GET /api/operations/overview` | MySQL / `ai_trace` / configuration | MyBatis read-only aggregate. |

Use the focused documents for the order, AI, and Redis step-by-step sequences.
