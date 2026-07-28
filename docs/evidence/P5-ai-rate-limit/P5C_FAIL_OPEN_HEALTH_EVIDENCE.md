# P5C FAIL_OPEN And Health Evidence

With the real Redis container stopped:

| Endpoint / behavior | Observed result |
| --- | --- |
| `/actuator/health` | HTTP 503, aggregate `DOWN`, `db=UP`, `redis=DOWN` |
| `/actuator/health/readiness` | HTTP 200, `UP`, `db=UP`, `readinessState=UP` |
| AI customer-service request | HTTP 200 through the local Mock Provider |
| Rate-limit mode | `degraded` |
| Quota display | No limit, remaining, or reset value displayed |
| Response evidence | Provider, Business Facts, Evidence, and six Trace steps present |

This keeps the Redis outage observable instead of hiding it. Readiness describes whether the Java application and its required MySQL storage can serve this Showcase, while the optional Redis limiter follows the documented `FAIL_OPEN` policy.

After Redis Compose was started again, its healthcheck became healthy. The first aggregate health poll was still HTTP 503 during the connection-recovery interval; the next poll was HTTP 200 with `db=UP` and `redis=UP`. Without restarting Java, the next AI request returned `X-RateLimit-Mode: redis` and `remaining=4`.
