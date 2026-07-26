# P4B Test Results

## Automated Results

| Verification | Result |
| --- | --- |
| MySQL clean rebuild | Passed with `./scripts/start-mysql.ps1 -Reset`; Docker healthcheck became healthy. |
| Flyway | Passed on H2 test database and real MySQL through V1-V8. |
| Java | `25 passed`: 11 P4 AI tests, 2 catalog tests, 11 P3 order tests, and 1 context test. |
| Python | `11 passed` with the local FastAPI TestClient. |
| Vue regression | `13 passed` existing Vitest tests. No P4 Vue code was added. |
| Vue production build | Passed: `vue-tsc --noEmit && vite build`. |

## Real Local Integration

Java ran against rebuilt local MySQL and FastAPI ran in `commerceflow-mock` / `MOCK` mode. The following requests completed through the real Java-to-Python path: price, gray-L stock, color/size, SKU code, purchasability, zero-stock purchasability, and an unsupported shipping question.

Each normal integration response contained real `availableStock=28` and `unitPrice="129.00"` for SKU `10004`, seven Java Evidence records, and six compact Trace steps. The zero-stock SKU `10005` returned a fact-bound non-purchasable answer. The shipping question returned `UNSUPPORTED_QUESTION`.

## Failure-path Integration

The local Python process was temporarily unavailable. Java returned `FALLBACK_ANSWER` with provider `java-fact-fallback` / `FALLBACK`, `fallbackUsed=true`, and Trace `error_code=AI_SERVICE_UNAVAILABLE`. Python was then restarted in `MOCK` mode. No external provider or API key was used.

## Initial Failures and Final State

The first Java run correctly applied V8 but an older P3 test expected exactly seven Flyway migrations. Its assertion was updated to the new V8 baseline. Later test setup failures were limited to Mockito stubbing and H2 metadata case matching; both were fixed in the tests. Final automated runs above passed.
