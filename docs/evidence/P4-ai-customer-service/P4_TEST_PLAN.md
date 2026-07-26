# P4 AI Customer Service Test Plan

## Planned Automated Coverage

| Layer | Planned count | Required focus |
| --- | ---: | --- |
| Java | 22 | Product/SKU loading and relation check; businessFacts shape; `BigDecimal` string price; stock `0` and missing context; off-sale/missing Product/missing SKU; blank/overlong/injection questions; Python success/timeout/unavailable/invalid JSON/provider error; Java Evidence source; response schema; trace steps; unsupported/fallback states; compact trace persistence. |
| Python | 12 | Deterministic Mock price/stock/color-size/purchasability/SKU-code; unsupported/missing facts; Pydantic input rejection; strict structured output; real-provider configuration error; provider error; deterministic repeat result. |
| Vue | 17 | Real Product/SKU selection/image failure; quick question; send/user/AI messages; loading; error/retry; unsupported; provider mode; Evidence; trace; fallback warning; no response before selection; plain-text rendering. |
| Browser acceptance | 1 | Real MySQL + Java + Python Mock + Vue at 1920x1080; selected SKU 10004, two-way visible messages, Evidence/Trace, image HTTP 200, zero console errors. |

**Estimated P4 total:** 52 verifications. Counts are a planning target, not a claim that tests already exist.

## Test Matrix Rules

- Java mock-server tests must prove Python cannot replace Java Evidence values.
- Tests must distinguish stock `0` from a missing/null fact.
- A same question may be deterministic in Mock mode; no real paid provider is invoked by default.
- Timeout, invalid JSON, and unavailable Python must produce explicit fallback/error status, not a fake ordinary answer.
- Flyway V8 tests are added only in the P4 implementation task after the migration is finalized.
