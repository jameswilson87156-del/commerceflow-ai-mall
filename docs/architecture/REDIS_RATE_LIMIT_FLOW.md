# Redis Rate Limit Flow

`POST /api/ai/customer-service/ask -> HMAC-derived local identity -> Redis Lua fixed window -> decision -> Java response`.

- Default local policy: five accepted AI requests per 60 seconds.
- Accepted requests carry rate-limit metadata. Rejected requests return HTTP 429, `Retry-After`, no-store semantics, and a stable body.
- Vue and UniApp use a single parsed cooldown state for all changing 429 text and disabled send/retry controls.
- Redis failure follows the documented Showcase `FAIL_OPEN` policy and is visible to the client/readiness contract.
- This is not production authentication, distributed-abuse protection, or a Redis participant in MySQL order transactions.
