# P7B Test Coverage Matrix

| Scenario | Evidence |
| --- | --- |
| Overview database aggregation and local boundary fields | `OperationsOverviewApiTests` |
| Empty trace aggregate and no sensitive runtime fields | `OperationsOverviewApiTests` |
| Read-only transaction contract | `OperationsOverviewService` implementation review and API tests |
| CORS allowlist / malicious origin rejection / exposed rate headers | `CorsConfigurationTests` |
| Overview real rendering and no GMV copy | `OperationsOverview.spec.ts` |
| Overview error and retry | `OperationsOverview.spec.ts` |
| Product/SKU/order/inventory/idempotency regressions | existing Java `OrderFlowTests` and related suite |
| Redis rate-limit regression | Java suite including `AiRateLimitRedisIntegrationTests` and `RateLimitUnitTests` |
| AI provider, evidence, trace contract | Java AI suite and `verify.ps1` normal Mock request |
| Admin/Mobile build and existing functional tests | module commands recorded in `P7B_TEST_RESULTS.md` |
| Runtime service identity and image HTTP checks | `status.ps1` / `verify.ps1 -IncludeMobile` manual runtime evidence |
| Blank MySQL transactional aggregates | isolated Compose evidence in `P7B_EMPTY_MYSQL_EVIDENCE.md` |

P7B intentionally does not add performance claims, load tests, production authentication tests, payment tests, logistics tests, or tests for features it does not implement.
