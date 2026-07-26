# P4D Test Results

Run date: 2026-07-26

| Area | Command or environment | Result |
| --- | --- | --- |
| Java | `./mvnw.cmd -f apps/mall-api/pom.xml test` | **28 passed**, 0 failures, 0 errors. |
| Java package | `./mvnw.cmd -f apps/mall-api/pom.xml package -DskipTests` | Passed. |
| Python | `services/ai-service/.venv/Scripts/python.exe -m pytest -q -p no:cacheprovider` | **11 passed**. |
| Vue | `npm.cmd test` in `apps/admin-web` | **33 passed** across 5 files. |
| Vue type check and build | `npm.cmd run build` in `apps/admin-web` | Passed: `vue-tsc --noEmit` and Vite production build. |
| MySQL / Flyway | Local Compose MySQL used by the isolated Java instance | 8 successful schema versions, latest version `8`. |

## Added regression coverage

- Java fallback coverage now verifies price, specification, SKU-code, and stock fallback answers when Python is unavailable.
- Java verifies unsupported shipping, refund, other-user-order, price-change, inventory-change, and injection-style questions do not return the selected SKU's demo stock `28` or price `129.00`.
- Java verifies fallback traces persist a category-only summary and all measured durations are non-negative.
- Java uses a minimal controlled provider delay to prove that `PROVIDER_COMPLETED` reflects elapsed provider work rather than a fixed value.
- Python extends its existing unsupported-boundary test with refunds, other-user orders, price changes, rule bypass, and JavaScript injection wording.
- Vue verifies unsupported fallback plus warning, fallback Trace styling, and backend-provided dynamic Trace duration rendering.

## Real local API checks

| Scenario | Observed result |
| --- | --- |
| Browser normal stock question, SKU `10004` | `commerceflow-mock`, `MOCK`, `fallbackUsed=false`, `ANSWERED`, 7 Evidence, 6 Trace steps. |
| Browser fallback stock question with Python stopped | `java-fact-fallback`, `FALLBACK`, `fallbackUsed=true`, `FALLBACK_ANSWER`, 7 Evidence, 6 Trace steps. |
| UTF-8 API shipping question with Python stopped | `java-fact-fallback`, `FALLBACK`, `fallbackUsed=true`, `UNSUPPORTED_QUESTION`; no `129.00` or `28` appears in the answer. |
| Recovery after Python restart | `commerceflow-mock`, `MOCK`, `fallbackUsed=false`, `ANSWERED`, 7 Evidence, 6 Trace steps. |

No hosted model, external model request, or real API key was used.
