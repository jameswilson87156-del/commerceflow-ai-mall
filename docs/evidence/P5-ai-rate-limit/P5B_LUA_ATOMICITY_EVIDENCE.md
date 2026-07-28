# P5B Lua Atomicity Evidence

`apps/mall-api/src/main/resources/redis/ai-rate-limit.lua` performs `INCR`, first-write `EXPIRE`, `TTL`, and remaining calculation in one Redis Lua execution. `EXPIRE` is only assigned when `INCR` returns one, so later requests do not slide the window.

`AiRateLimitRedisIntegrationTests.concurrentChecksNeverAllowMoreThanConfiguredLimit` submits 12 concurrent checks for one identity against real Compose Redis. Exactly five decisions are allowed. This proves the P5 first-window enforcement does not depend on a Java `GET`/`SET` race.

Real browser recovery also crossed a Redis window boundary: the 429 countdown ended, send re-enabled, and a later recovered request returned Redis mode with remaining four.
