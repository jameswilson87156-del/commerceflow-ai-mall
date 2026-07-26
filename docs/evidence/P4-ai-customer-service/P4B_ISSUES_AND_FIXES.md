# P4B Issues and Fixes

## Flyway Count Assertion

- Symptom: the first Java regression run failed because a P3 test expected seven successful Flyway migrations.
- Root cause: P4B correctly added V8, but the historic count assertion had not been advanced.
- Fix: renamed the test to describe the full migration baseline and expected eight migrations.
- Regression: final Java suite passed with V1-V8.

## P4 Test Stubbing and H2 Metadata

- Symptom: early P4 tests produced null request arguments while replacing a default Mockito stub; the migration-column check also missed H2's lower-case metadata names.
- Root cause: the prior matching stub executed during replacement, and the metadata assertion assumed uppercase names.
- Fix: reset before scenario-specific stubs, use type-safe argument capture, and compare metadata names case-insensitively.
- Regression: all 11 P4 Java tests passed.

## Local Port Collision

- Symptom: ports `8080` and then `8082` were already occupied by unrelated local processes; the first attempted Java instance did not start, and its health endpoint belonged to another service.
- Root cause: local development port contention.
- Fix: left existing services untouched and ran this P4B Java instance on `8091`; OpenAPI confirmed the new ask route before integration requests.
- Regression: real Java-to-Python requests and fallback verification completed on `8091`.

No production defect, external-provider failure, or database migration failure was observed after these fixes.
