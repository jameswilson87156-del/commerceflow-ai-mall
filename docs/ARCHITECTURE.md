# Architecture

CommerceFlow AI Mall V1 is a modular monolith with one separate AI side service.

```text
UniApp / Vue
      -> mall-api (Java 17, Spring Boot 3)
      -> MySQL + Flyway
      -> CustomerServiceProvider (FastAPI Mock or server-side OpenAI-compatible adapter)
```

Java owns Product, SKU, Inventory, Cart, Order, money, transactions, and the authoritative `businessFacts` payload. `CurrentUserPort` is the consumer identity boundary; the separate `OperatorScope` is the prerequisite for cross-user management reads. The selected Provider receives only the facts required for the question and returns a structured answer suggestion. FastAPI cannot access MySQL, modify inventory, or call Java back; external Provider keys remain server-side.

The backend evolution starts with E0 contract freeze, Phase 0-B interface/identity closure, E1 user-scope isolation, and E2 order reliability boundaries. Current `/api/...` routes, the CNY-only money contract, the single `CREATED` order status, and the typed AI answer/trace shape are protected by contract tests. Phase 0-B adds explicit `/api/v1/me/...` consumer routes and `/api/v1/operator/...` management routes; the Demo adapters are explicit local/test fixtures, not production authentication. E2 adds port-based inventory/idempotency/order writes and a pending transactional Outbox event; it does not imply a broker, worker, or production delivery guarantee. E3 adds a server-side OpenAI-compatible provider boundary and low-cardinality metrics. E4 adds staging deployment assets and environment validation; neither phase implies paid-provider, production-authentication, backup-recovery, DNS, or public-runtime evidence.

## Order Consistency

The order transaction validates SKU state, uses `UPDATE inventory SET available_stock = available_stock - ? ... AND available_stock >= ?`, writes the order and snapshots, writes the inventory log, and clears the cart. Zero affected rows means the condition no longer holds; the transaction rolls back if any later write fails.

## Honest Boundary

No real payment, logistics, merchant settlement, production SLA, or public deployment is included. Mock Provider works without credentials; the OpenAI-compatible adapter is locally contract-tested but has not been run against a paid external model. Showcase implementation and learning ownership are tracked separately.
