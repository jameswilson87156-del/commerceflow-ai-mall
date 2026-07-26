# P4C Test Results

Run date: 2026-07-26

## Automated Results

| Area | Command or environment | Result |
| --- | --- | --- |
| Java | Isolated copy of `apps/mall-api`, `mvn -f .../pom.xml test` | **25 passed**, 0 failures, 0 errors. |
| Flyway | Java test empty H2 database | Validated and applied **V1-V8**. |
| Python | `services/ai-service/.venv/Scripts/python.exe -m pytest -q -p no:cacheprovider` | **11 passed**. |
| Vue tests | `npm.cmd test` in `apps/admin-web` | **31 passed** across 5 files. |
| Vue type check and production build | `npm.cmd run build` in `apps/admin-web` | Passed; `vue-tsc --noEmit` and Vite production build succeeded. |

The primary source `apps/mall-api/target` was held by an already-running older local Java service on port 8081. It was not stopped or changed. Java tests therefore ran from an isolated mechanical copy of the same source with `target` excluded. The test copy is not a project artifact.

## Real Local API Matrix

Endpoint: `POST http://127.0.0.1:8091/api/ai/customer-service/ask`.

| Scenario | skuId | Expected and observed result | Evidence | Trace |
| --- | ---: | --- | ---: | ---: |
| Price question | 10004 | `ANSWERED`, `commerceflow-mock`, `MOCK` | 7 | 6 |
| Color and size question | 10004 | `ANSWERED`, `commerceflow-mock`, `MOCK` | 7 | 6 |
| SKU-code question | 10004 | `ANSWERED`, `commerceflow-mock`, `MOCK` | 7 | 6 |
| Purchasability question | 10004 | `ANSWERED`, `commerceflow-mock`, `MOCK` | 7 | 6 |
| Zero-stock purchasability question | 10005 | `ANSWERED`, `availableStock=0`, `commerceflow-mock`, `MOCK` | 7 | 6 |
| Shipping question | 10004 | `UNSUPPORTED_QUESTION`, `commerceflow-mock`, `MOCK` | 7 | 6 |

All six requests were sent to local Java, which called the local deterministic Python provider. No hosted model, external network model call, or real API key was used.

## Fallback and Recovery

With the local Python service deliberately stopped, Java returned `FALLBACK_ANSWER` with provider `java-fact-fallback` and mode `FALLBACK`. The fallback screenshot was captured from that real condition. After Python was restarted, normal responses again returned `commerceflow-mock` and `MOCK`.

