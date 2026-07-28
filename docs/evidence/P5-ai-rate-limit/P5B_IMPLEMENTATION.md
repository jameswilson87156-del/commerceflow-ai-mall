# P5B Implementation

P5B adds a Redis fixed-window guard only to `POST /api/ai/customer-service/ask`: five accepted requests per 60 seconds for one privacy-preserving local identity.

Effective order: request validation, Redis guard, Product/SKU/Inventory facts, existing Java-to-Python call, existing Java Evidence/Trace persistence. A 429 returns before catalog, Python, Evidence, or Trace work.

- `spring-boot-starter-data-redis` uses default Lettuce through `StringRedisTemplate`.
- `RedisAiRateLimiter` executes `redis/ai-rate-limit.lua`.
- Key form: `commerceflow:local:ai:rate:v1:{hmac-truncated}:windowStart`.
- Identity is HMAC-SHA-256 of `userId|remoteAddress`, truncated to 24 lowercase hex characters; raw user ID and address are absent from Redis.
- The script sets `EXPIRE` only when the counter is created; later requests do not extend TTL.
- Redis success exposes `X-RateLimit-Mode`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`.
- 429 includes `Retry-After` and `AI_RATE_LIMIT_EXCEEDED`.
- Default `FAIL_OPEN` continues the existing AI path with `mode: degraded` and no invented quota. Explicit `FAIL_CLOSED` produces controlled 503.

`docker-compose.yml` pins `redis:8.0.2`, maps 6380, has a `redis-cli ping` healthcheck, disables persistence, and uses no password by default. `.env.example`, `start-redis.ps1`, `demo-ready.ps1`, `verify.ps1`, CI, and Java configuration were updated.

Real browser validation exposed a CORS gap: Java returned custom headers but Vue could not read them cross-origin. Existing local CORS now exposes the rate-limit headers and `Retry-After`.

No Flyway migration, product/order/inventory/MyBatis change, persistent rate-limit table, Python Provider-rule change, external model call, or real API key was introduced.
