# P5B Issues and Fixes

| Observation | Root cause | Resolution | Evidence |
| --- | --- | --- | --- |
| Maven Wrapper was not found from `apps/mall-api`. | Wrapper is at repository root. | Use `./mvnw.cmd -f apps/mall-api/pom.xml ...`. | Final Java suite passed. |
| Testcontainers could not discover the local Docker Desktop API. | Local Docker API/pipe compatibility. | Do not claim Testcontainers coverage; use real Docker Compose Redis locally and a GitHub Actions Redis service in CI. | Five non-skipped Redis integration tests passed. |
| Vue initially showed a safe disabled card even though direct Java response had Redis headers. | CORS did not expose custom headers. | Expose rate-limit headers and `Retry-After` in existing local CORS configuration. | Browser showed Redis 5/4 afterward. |
| Initial P5 Trace count included earlier H2 context data. | Shared test context. | Delete `ai_trace` in P5 test setup. | Final Java: 36 passed. |
| Existing P3 order test once collided on a millisecond order number. | Existing test timing / order number implementation. | Recorded only; P5 scope does not alter order behavior. | Isolated and final full suite passed. |

No issue was invented for documentation. No Flyway/database repair was required.
