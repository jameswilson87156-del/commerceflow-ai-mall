# P5 Final Freeze

## Frozen scope

- Final branch: `feat/p5-ai-rate-limit-final-freeze`
- Frozen implementation HEAD: `dd197eac897e26f88999dbf507ed4e6d6ec68ce9` (`fix: unify rate limit countdown and freeze p5`).
- This documentation record follows in a separate commit so it can refer to the exact immutable implementation HEAD.
- Scope: Redis protection for the AI customer-service HTTP endpoint only. P5 does not change products, carts, orders, inventory, MySQL transactions, or the Python provider contract.
- Further P5 work is limited to reproducible defects. New rate-limit capabilities require a later approved phase.

## Runtime design

| Decision | Frozen behavior |
| --- | --- |
| Redis | Redis `8.0.2`, Compose-bound to `127.0.0.1:6380` for the local Showcase. |
| Counter | One Lua-scripted atomic fixed window. |
| Identity | HMAC-derived local identity key from the demo user and remote address; no raw identity is stored in the Redis key. |
| Default rule | 5 allowed requests per 60-second window. |
| Rejection | Java returns HTTP `429`, a `Retry-After` header, the stable response body contract, and no-store cache behavior. |
| Availability policy | Redis storage failure uses the documented `FAIL_OPEN` Showcase policy. |
| Readiness | The readiness contract reports Redis availability without pretending that a degraded rate limiter is healthy. |

## P5D 429 presentation

The backend 429 contract remains unchanged. The Vue workbench now initializes one `cooldownSeconds` state from `Retry-After`; only when that header is absent does it use `body.retryAfterSeconds`, and only when both are absent does it use the documented one-second safe fallback.

All dynamic 429 text reads that one state: the top status line, the error card, the retry button, and the send button. The error card deliberately does not render the backend's stale, response-time message. The captured real 429 used `57` seconds in every dynamic location.

During a 429, the retained user question remains visible, while Provider returns to a waiting state and Evidence and Trace show their real empty states. No sixth answer, Evidence, or Trace is manufactured. A response after cooldown expiry succeeds again.

## Final verification

| Area | Result |
| --- | --- |
| Flyway | V1-V8 verified by the Java test suite. |
| Java | 38 tests passed. |
| Real Redis integration | 5 tests passed as part of the Java suite. |
| Python | 11 tests passed. |
| Vue | 43 tests passed. |
| Vue type check | Passed. |
| Vue production build | Passed. |
| Real browser run | Five browser-context requests succeeded; the sixth returned 429; after the cooldown a new request succeeded. |
| Trace integrity | `ai_trace` changed by +5 before the 429, not by the rejected request, then by +1 after recovery. |
| 429 capture | `1920x1080`, DPR 1, `visualViewport.scale` 1, 272px sidebar, no crop or stretch. |
| Product assets | Six rendered local product images had nonzero natural dimensions. |
| Console | Chrome page-console capture contained 0 application error entries; only Vite debug entries were present. Chrome CDP's low-level Log domain separately classifies the expected HTTP 429 resource response as a network-status entry, not an unhandled application error. |

## Showcase images

- `screenshots/v2/05-ai-rate-limit-showcase-final.png`: normal Redis-protected Mock response.
- `screenshots/v2/05-ai-rate-limit-429-showcase-final.png`: final real sixth-request rejection with the unified countdown.
- `screenshots/v2/05-ai-rate-limit-fail-open-final.png`: Redis-unavailable, documented FAIL_OPEN state.

All three are local Showcase evidence. They use the local Java API, MySQL, Redis where applicable, local FastAPI `commerceflow-mock`, original local product assets, no external network, and no real API key.

## Explicit boundaries

- The current HMAC identity key is not production authentication.
- This fixed-window limit does not provide DDoS protection.
- Redis does not participate in orders, inventory updates, or MySQL transactions.
- P5 does not add real payment, logistics, refunds, addresses, coupons, remote AI providers, or a public deployment.
