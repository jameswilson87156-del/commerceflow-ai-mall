# P5B FAIL_OPEN Evidence

With Vue, Java, MySQL, and local FastAPI still running, Redis was stopped. A valid Vue request completed through the existing AI path with mode `degraded`.

- UI text: `限流保护暂时降级` and `未显示未经证实的剩余额度`.
- No limit, remaining, reset, or synthetic success metric was displayed.
- Redis was restarted, reached `healthy`, returned `PONG`, and the next browser request returned normal Redis mode with remaining four.

The local Showcase defaults to fail-open so the optional counter outage does not hide the demo path. Explicit fail-closed behavior is covered by `RateLimitUnitTests.redisFailureHonorsFailClosedWhenExplicitlyConfigured`.

Real screenshot: `screenshots/v2/05-ai-rate-limit-fail-open-real.png`.
