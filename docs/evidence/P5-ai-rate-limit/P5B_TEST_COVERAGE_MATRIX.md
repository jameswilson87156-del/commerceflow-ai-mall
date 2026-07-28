# P5B Test Coverage Matrix

| Scenario | Evidence | Type | Result |
| --- | --- | --- | --- |
| Disabled limiter does not access Redis | `RateLimitUnitTests.disabledLimiterNeverAccessesRedis` | Automated | Passed |
| Five accepted then 429 with headers/body | `AiRateLimitRedisIntegrationTests.allowsFiveThenReturns429WithHeadersWithoutCallingProviderAgain` | Real Redis | Passed |
| Rejection skips Provider and Trace | Same integration test verifies five Provider calls/five traces | Real Redis | Passed |
| TTL fixed-window behavior | `keepsOneTtlAndSharesQuotaAcrossSkuButSeparatesRemoteIdentities` | Real Redis | Passed |
| Same identity shares quota across SKU | Same integration test | Real Redis | Passed |
| Different identity separates quota | Same integration test | Real Redis | Passed |
| Invalid basic request does not consume quota | `invalidInputDoesNotConsumeButValidMissingProductDoesConsume` | Real Redis | Passed |
| Valid catalog failure is charged | Same integration test | Real Redis | Passed; order is validation -> limiter -> facts |
| Unsupported request is charged | `unsupportedAndProviderFallbackRequestsStillConsumeRedisQuota` | Real Redis | Passed |
| Provider failure / Java fallback is charged | Same integration test | Real Redis | Passed |
| 12 concurrent checks allow only five | `concurrentChecksNeverAllowMoreThanConfiguredLimit` | Real Redis | Passed |
| Fail-open never invents quota | `redisFailureUsesFailOpenWithoutInventingQuota` plus browser stop/restart | Automated/manual | Passed |
| Explicit fail-closed | `redisFailureHonorsFailClosedWhenExplicitlyConfigured` | Automated | Passed |
| Vue normal, 429, degraded | P5 Vue tests | Automated | Included in 38 passed Vue tests |
| Vue countdown recovery | Real browser | Manual reproducible | Passed |
| CORS header visibility | Origin assertion plus browser | Automated/manual | Passed |
| Images / console | CDP capture | Manual reproducible | Five HTTP 200 assets; console 0 |

Testcontainers was not represented as passing coverage because local Docker API discovery failed. P5B instead uses Docker Compose Redis locally and a Redis service in CI.
