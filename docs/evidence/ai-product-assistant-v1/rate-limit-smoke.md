# Redis Lua rate-limit smoke

The isolated runtime used the local `MOCK` provider. The existing Redis Lua fixed-window implementation was not changed.

- First five requests for one synthetic identity: HTTP 200 with Redis rate-limit headers.
- Sixth request: HTTP 429.
- `Retry-After`: present.
- Limit header: 5.
- Remaining header on rejection: 0.
- Trace count changed by exactly five after the allowed requests.
- The sixth request created no additional Trace and did not reach the provider.
- No remote provider call was used.

Redis remains outside Order, Inventory, idempotency, and MySQL transaction handling.