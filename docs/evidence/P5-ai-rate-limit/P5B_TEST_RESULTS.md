# P5B Test Results

| Area | Result |
| --- | --- |
| MySQL clean rebuild | `scripts/start-mysql.ps1 -Reset`; Flyway V1-V8 applied. |
| Redis | Compose Redis 8.0.2 healthy; `PONG`. |
| Java | `./mvnw.cmd -f apps/mall-api/pom.xml test`: 37 passed, 0 failed, 0 skipped. |
| P5 Redis integration | 5 passed against real Compose Redis on 6380. |
| P5 rate-limit units | 4 passed. |
| Vue | `npm test -- --run`: 38 passed. |
| Vue build | `npm run build`: passed. |
| Python regression | 11 passed; no Python code changed. |
| Browser | Normal Redis, 429, FAIL_OPEN, recovery: console errors 0. |
| Local product assets | Cache-cleared CDP capture: five image requests HTTP 200. |

Before the final green Java run, the P5 integration test initially counted `ai_trace` rows from another shared H2 context; it now clears that table in its own setup. A pre-existing P3 millisecond order-number collision also appeared once in an earlier full suite; it passed in isolation and in the final full suite. P5 does not modify order behavior.
