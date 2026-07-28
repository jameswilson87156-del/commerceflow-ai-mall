# P5C Implementation

## Minimal changes

- `RateLimitHeaders.rejected` now sends `Cache-Control: no-store` in addition to the existing Retry-After and rate-limit headers.
- `RateLimitExceededException` now formats `请求过于频繁，请在 N 秒后重试。` from the actual limiter decision.
- `docker-compose.yml` binds Redis only to the loopback interface. It continues to use Redis 8.0.2, no persistence, no password, and the existing healthcheck.
- Actuator now exposes component statuses and defines:
  - `/actuator/health`: Java, MySQL, and Redis are visible; Redis down makes aggregate health `DOWN`.
  - `/actuator/health/readiness`: Java readiness state plus MySQL; optional Redis is intentionally excluded for the documented `FAIL_OPEN` behavior.
- The Vue workbench uses a 1920 desktop grid whose SKU list, chat list, and right-side fact/evidence/trace areas scroll independently.

## Chat behavior

`AiCustomerServiceWorkbench.vue` holds a ref to the message container. New local user/loading messages always move that container to the newest entry. A completed answer, network error, 429, retry, or fallback follows only when the reader was already at the bottom. The page window is never scrolled and the composer never receives focus programmatically from this behavior.

## Rate-limit presentation

The center-column status strip receives only response headers and body values from the Java API. It shows normal Redis limit, remaining, and reset data; the 429 state shows the retained question, remaining zero, a Retry-After countdown, and the disabled send/retry controls. `degraded` shows no quota values.

No order, inventory, catalog, or Python Provider business implementation was changed in P5C.
