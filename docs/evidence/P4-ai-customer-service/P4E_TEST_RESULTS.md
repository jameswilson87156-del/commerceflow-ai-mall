# P4E Test Results

## Automated Results

| Area | Command | Result |
| --- | --- | --- |
| Java | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 28 passed, 0 failures, 0 errors |
| Python | `services/ai-service/.venv/Scripts/python.exe -m pytest -q -p no:cacheprovider` | 11 passed |
| Vue | `npm.cmd test` | 35 passed across 5 files |
| Vue type check and production build | `npm.cmd run build` | passed |
| Java package | `./mvnw.cmd -f apps/mall-api/pom.xml package -DskipTests` | passed |

## Database Rebuild

`scripts/start-mysql.ps1 -Reset` removed the local Compose demo volume and recreated MySQL. The restarted Java application applied Flyway V1 through V8 successfully. The real MySQL history query returned `8` successful versions and maximum version `8`.

The post-rebuild product API returned two Showcase Products and five SKUs. The normal P4 request returned seven Evidence entries and six Trace steps. After the fallback capture, Python was restarted and a real Java request again returned `commerceflow-mock / MOCK`, `fallbackUsed=false`, seven Evidence entries, and six Trace steps.

## P4E Frontend Coverage

- Existing 33 P4D Vue tests still pass.
- New tests assert that selector, chat, and facts/evidence panels remain mounted together.
- A deliberately long Trace ID retains its full `title` value and uses the local overflow-safe class.
- A longer customer question is rendered as user text and still receives the API-provided answer; the test does not invent a response.
- Existing normal and fallback tests continue to cover `commerceflow-mock / MOCK`, `java-fact-fallback / FALLBACK`, warnings, six Trace steps, dynamic durations, retry, loading, empty, and error states.
