# P5C Test Results

All commands ran locally after an empty MySQL Compose rebuild with Flyway V1-V8 and Redis 8.0.2 healthy.

| Area | Command | Result |
| --- | --- | --- |
| Java full suite | `./mvnw.cmd -f apps/mall-api/pom.xml test` | 38 passed |
| Redis integration | Included in Java suite: `AiRateLimitRedisIntegrationTests` | 5 passed against real Compose Redis |
| Readiness contract | Included in Java suite: `HealthReadinessTests` | 1 passed with Redis deliberately unreachable |
| Python | `services/ai-service/.venv/Scripts/python.exe -m pytest -q` | 11 passed |
| Vue | `npm test -- --run` | 39 passed |
| Vue type check | `npx vue-tsc --noEmit` | passed |
| Vue production build | `npm run build` | passed |

The real Redis tests continue to cover five allowed requests, the sixth rejection, shared identity behavior, invalid-input ordering, fallback consumption, and concurrent Lua checks. P5C adds coverage for `Cache-Control: no-store`, actual retry-second wording, header/body consistency, and the readiness contract.
