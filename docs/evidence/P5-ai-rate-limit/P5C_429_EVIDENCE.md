# P5C 429 Evidence

## HTTP contract

A rejected request returns:

- HTTP `429`
- `Cache-Control: no-store`
- `Retry-After` equal to `retryAfterSeconds` in the JSON body
- `X-RateLimit-Mode: redis`
- `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`
- JSON `code`, `message`, `retryAfterSeconds`, `limit`, `remaining`, and `timestamp`

The error message is now server-derived, for example `请求过于频繁，请在 46 秒后重试。` It contains no Redis host, port, key, Lua, or stack detail.

## Real browser sequence

1. Redis was flushed with `FLUSHDB`; no quota was faked in Redis.
2. In the same Chrome page context as the UI, five real `fetch` calls used the page API address `http://localhost:8081/api/ai/customer-service/ask`, user 1, one remote identity, and five distinct `clientRequestId` values.
3. The five real Java responses were HTTP 200 with remaining values `4`, `3`, `2`, `1`, and `0`.
4. The page itself submitted the sixth request. It returned HTTP 429 and did not create a successful sixth AI answer, Evidence, or Trace.

The first dry-run used `127.0.0.1` rather than the UI API host `localhost`. On this machine the two addresses did not share the same remote-address digest. The final sequence corrected that real mismatch by using the exact page API host.

The final 429 UI retained the question, showed limit 5, remaining 0, Retry-After countdown, reset time, disabled send text, and a disabled retry action. At the real expiry boundary, send was enabled again; a new request succeeded with Redis `remaining=4` without restarting Java.
