# AI Product Assistant V1 evidence

## Scope

This evidence records the AI1 provider adapter work only. Java remains the authoritative source for Product, SKU, and Inventory facts; FastAPI validates a constrained structured provider answer; Java owns Evidence, Trace persistence, Redis admission, and fact-based fallback.

## Results

- Offline Python adapter tests: passed.
- Java regression: passed; the MySQL-only reliability suite remains conditionally skipped in the ordinary local command.
- Mock showcase verification: passed.
- Local provider-failure smoke: verified both fallback-enabled and fallback-disabled behavior without a remote provider call.
- Redis admission smoke: verified five allowed mock requests followed by one HTTP 429 without an additional Trace.
- Initial shared-provider synthetic smoke: attempted once before AI1.1/AI1.2 and did not meet the grounded success contract. It was not retried.
- AI1.3 final synthetic smoke: one newly started end-to-end request completed through the real compatible-provider path. The strict schema, local intent, full claims, Java-owned Evidence, Trace, and grounded renderer checks passed; fallback was not used.

This is not a production-stability claim. No production data, merchant data, order data, provider credential, endpoint, model value, raw upstream response, or local absolute path is recorded here.
