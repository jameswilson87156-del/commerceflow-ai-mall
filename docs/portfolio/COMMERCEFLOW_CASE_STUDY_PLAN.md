# CommerceFlow Case Study Plan

## Accurate Narrative

CommerceFlow AI Mall is a local job-search Showcase for AI application, Java full-stack, Java backend, and Python AI application roles. It demonstrates a Java modular monolith with transactional commerce flows, a bounded FastAPI AI provider, Redis request limiting, Vue admin pages, and UniApp H5 flows.

## Case Study Sections

1. Problem: demonstrate one connected commerce-to-AI flow without inventing commercial scale.
2. Scope: product/SKU/inventory, cart/order evidence, grounded AI, rate limit, two frontends.
3. Architecture: Java fact owner, FastAPI structured provider, MySQL, Redis only for AI protection.
4. Order hard part: BigDecimal, snapshots, conditional stock update, transaction rollback, idempotency.
5. AI hard part: restricted facts, provider validation, Java fallback, Evidence/Trace.
6. Rate-limit hard part: Redis Lua, 429 cooldown, FAIL_OPEN boundary.
7. Evidence: selected canonical runtime screenshots and phase freeze links.
8. Boundaries, ownership disclosure, and future learning rebuild.

Do not present commercial customers, production deployment, high concurrency metrics, DAU/GMV, revenue impact, real payments/logistics, microservices, vector search, or external model operation.
