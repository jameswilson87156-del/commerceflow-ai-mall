# CommerceFlow Interview Checklist

- Explain Product, SKU, Inventory, Cart, Order, OrderItem, and inventory movement without notes.
- Derive why `available_stock >= quantity` prevents negative stock and why zero affected rows fails the request.
- Explain transaction rollback and idempotency replay/conflict behavior.
- Describe snapshot fields and why current product data cannot replace historical order facts.
- Draw Java -> businessFacts -> FastAPI -> Java Evidence/Trace, including the forbidden callback/database paths.
- Explain Mock, fallback, 429, Retry-After, Redis fixed window, HMAC identity summary, and FAIL_OPEN limits.
- Start the local runtime, inspect health, and identify each process/port.
- Navigate one admin and one mobile H5 flow with real local data.
- State all current boundaries: no auth, payment, logistics, refund, addresses, coupons, real provider key, public deployment, or device-runtime claim.
- State exactly where Codex assisted and what must be rewritten to demonstrate ownership.
