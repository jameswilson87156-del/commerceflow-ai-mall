# P6C FAIL_OPEN Evidence

The local Redis container was stopped while Java, MySQL, Python, and the H5 page remained available. A normal mock answer completed with `X-RateLimit-Mode: degraded`. The mobile page displayed “限流保护暂时降级” and did not show fake limit, remaining, or reset values.

Redis was then restarted. A subsequent Java request returned `X-RateLimit-Mode: redis` with real quota headers, confirming recovery without changing the mobile page implementation.

Screenshot: `screenshots/v2/07-mobile-ai-customer-service-fail-open-real.png`.
