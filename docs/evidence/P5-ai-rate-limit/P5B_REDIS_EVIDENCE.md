# P5B Redis Evidence

- Image/version: `redis:8.0.2`.
- Local endpoint: `localhost:6380`; Compose healthcheck: `redis-cli ping`.
- Observed health after recovery: `PONG`.
- Observed key shape, redacted: `commerceflow:local:ai:rate:v1:{<24-char-hmac>}:<windowStart>`.
- Observed active counter: `1`; observed positive TTL: 30 seconds, bounded by the configured 60-second window.
- Redis stores only counter and TTL. It stores no raw user ID/address/question, product/SKU, Evidence, Trace, order/cart data, or API credential.

The real browser run used the local Compose instance. The isolated test class flushes only its local test Redis database before each test.
