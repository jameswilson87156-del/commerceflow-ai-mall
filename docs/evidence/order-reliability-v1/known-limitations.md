# Known limitations

- This is a local MySQL correctness suite, not a QPS, latency, failover, or production-capacity benchmark.
- It verifies one application process and one isolated MySQL container; distributed multi-instance idempotency behavior is outside this evidence.
- The in-progress idempotency response is an allowed transient outcome during the first request.
- No payment, shipping, refund, address, coupon, or Redis inventory logic is tested.
