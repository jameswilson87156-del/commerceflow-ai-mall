# P7 CI Gap Analysis

## Current Workflow

`.github/workflows/ci.yml` uses Java 17, Python 3.12, and Node 20. It runs Java tests with Redis, Python tests, and admin/mobile builds. Maven and npm caches are configured for the applicable jobs.

## Gaps

| Gap | P7C action |
| --- | --- |
| No MySQL service or empty-schema/Flyway verification in CI | Add MySQL service and run the Java test path that proves Flyway V1-V8. |
| Admin build but no Vitest job | Run admin `npm test` before build. |
| Mobile builds but no Node test command | Run mobile `npm test`, H5 build, and `build:uni`. |
| No explicit provider safety | Force Mock/default-safe variables; do not provide real provider keys. |
| No repository secret/file gate | Add a lightweight tracked-file/secret-pattern check. |
| No screenshot/asset link check | Validate canonical screenshot existence, dimensions, and referenced local asset paths; do not generate screenshots in CI. |
| No release matrix artifact | Print a concise component/result summary. |

Keep the workflow small: services, tests, builds, static safety checks. Do not introduce a remote AI model, browser farm, or elaborate deployment pipeline for Showcase V1.
