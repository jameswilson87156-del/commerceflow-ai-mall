# P5B 429 Evidence

The running Vue workbench sent five accepted requests and then a sixth valid question for the same local identity.

- Result: HTTP 429, `AI_RATE_LIMIT_EXCEEDED`.
- Headers: `Retry-After`, `X-RateLimit-Mode: redis`, `X-RateLimit-Limit: 5`, `X-RateLimit-Remaining: 0`, and `X-RateLimit-Reset`.
- UI: retains the question, shows a countdown, disables send/retry, and appends no fabricated assistant answer.
- Automated proof: exactly five Provider calls and exactly five `ai_trace` rows after the rejected sixth request.

Real screenshot: `screenshots/v2/05-ai-rate-limit-429-real.png`.
