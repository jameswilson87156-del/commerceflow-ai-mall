# P6C Mobile AI Customer Service Implementation

## Scope

P6C adds one UniApp H5-first page: `pages/ai/customer-service`. It is reachable from product detail only after a real SKU has been selected. Navigation carries only `productId` and `skuId`; the AI page reloads the product and resolves the SKU from `GET /api/products/{productId}`.

The page sends only `userId`, `productId`, `skuId`, `question`, and `clientRequestId` to `POST /api/ai/customer-service/ask`. It never supplies price, stock, status, provider, Evidence, Trace, or businessFacts.

## UI behavior

- Product context uses the reloaded Product/SKU response and local project asset path.
- Quick questions cover price, color/size, stock, purchasability, and SKU code.
- Chat is turn-local in memory only. It is not persisted or sent as conversation history.
- Success facts, Evidence, Trace, provider metadata, fallback status, and latency are rendered solely from the Java response.
- Evidence and Trace are collapsed by default. The chat panel scrolls internally to new results; the multiline composer remains fixed above the safe area.
- `429` uses one `cooldownRemaining` source for the status bar, error card, disabled quick actions, retry action, and send action.
- `X-RateLimit-Mode: degraded` displays a FAIL_OPEN disclosure and intentionally omits quota numbers.

## Boundaries

No Java, Python, Redis Lua, Flyway, MySQL, order, inventory, or idempotency logic changed in P6C. The mobile page has no login, payment, logistics, refund, coupon, vector search, external model, or chat persistence feature.

The product-detail SKU selector now permits a zero-stock SKU to be selected for truthful AI consultation; add-to-cart remains controlled by the existing purchasability check.
