# P6C Test Results

## Mobile

- `npm test`: 28 passed, 0 failed.
- `npm run build`: passed.
- `npm run build:uni`: passed.

## Local runtime

- MySQL Product 101 / SKU 10004 was queried through Java.
- Java response carried real local price `129.00 CNY` and inventory `24`.
- Python normal mode: `commerceflow-mock / MOCK`.
- Python stopped: `java-fact-fallback / FALLBACK`.
- Redis stopped: Java returned degraded rate-limit mode and page showed FAIL_OPEN disclosure without quota values.
- Redis restarted: local Java API returned `X-RateLimit-Mode: redis` and real remaining quota.

## Full regression

- Java: 43 passed, including 5 real Redis integration tests.
- Python: 11 passed.
- Admin Vue: 43 passed; production build passed.
- Mobile UniApp: 31 passed; H5 production build and non-H5 `uni build` passed.
- Clean local MySQL rebuild: a new Docker volume applied Flyway V1 through V8 successfully; the Java health endpoint then reported UP.

P6C does not change Java, Python, Redis, Flyway, or admin Vue source.

## P6C.1 final verification

- HTTP 429 retained `现在还有库存吗？` in the composer and in the rate-limit card; the composer showed `8/500` rather than an empty draft.
- The fifth successful request remained chat history only. No sixth successful answer, Evidence, or Trace was produced for the rejected request.
- After cooldown, manual retry sent the retained text with a new `mobile-<uuid>` client request ID and then cleared the draft on success.
- Final local screenshots use 390x844, DPR 1, zh-CN, zoom 100%.
