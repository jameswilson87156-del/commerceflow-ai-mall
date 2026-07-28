# P5C Code Review Findings

## P5B baseline retained

The following P5B implementation was reviewed and retained without a rewrite:

- `redis/ai-rate-limit.lua` remains the atomic fixed-window `INCR` plus first-write `EXPIRE` implementation.
- The default is still five requests per 60-second window.
- `IdentityKeyResolver` still derives a truncated HMAC digest instead of storing a raw user id or remote address in Redis keys.
- `AiService` validates the request before checking the limiter; a rejected request does not load catalog facts, call Python, or persist an AI trace.
- `FAIL_OPEN` still returns `degraded` without a fabricated limit, remaining quota, or reset time.
- P4 answer, Evidence, Trace, and Java fact-fallback behavior remain intact.

## Gaps found and resolved

| Finding | Resolution |
| --- | --- |
| The P5B 429 response could be cached and used a generic retry message. | Added `Cache-Control: no-store` and a server-derived retry-second message. |
| Redis was published on every host interface. | Bound only Redis to `127.0.0.1:${REDIS_PORT:-6380}:6379`; MySQL mapping was not changed. |
| A Redis outage made aggregate Actuator health `DOWN`, but there was no explicit readiness contract for optional limiter storage. | Kept Redis visible in aggregate health and added readiness containing only Java readiness state and database health. |
| The desktop chat history could expand the page as messages accumulated. | Converted the three desktop panels to viewport-relative grid areas with independent vertical scrolling. |
| The P5B capture file was 1920x1080 but used an effectively 1.5x scaled browser surface. | Replaced the capture environment; details are in `P5C_CAPTURE_GAP_ANALYSIS.md`. |

## Explicit boundary

The current identity digest is a local Showcase discriminator, not production authentication or an authorization boundary. `FAIL_OPEN` is acceptable here only because this is the deterministic Mock Showcase; a production policy requires separate availability, abuse, and identity decisions.
