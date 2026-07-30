# AI1.2 real-provider smoke readiness

## Scope and result

This is an offline readiness correction. Real external calls in this phase: **0**. The first real smoke remains **FAILED** and is not rewritten as a pass. A separately approved follow-up may perform at most one synthetic request.

## Corrected blockers

- Python has a bounded remote timeout budget: connect 2 seconds, read 10 seconds, write 10 seconds, and pool 2 seconds.
- Java waits 15 seconds for the local FastAPI response, leaving a 5-second margin beyond the remote read budget. There is no unbounded wait or automatic retry.
- Provider configuration now resolves each field independently: project, shared portfolio, legacy compatibility, then safe default/fail-closed behavior. Empty higher-priority values do not mask lower-priority values.
- The Chat Completions payload now contains only `model` and `messages`; optional provider-specific fields are omitted.
- The system prompt requires exactly one JSON object, full strict claims, allowed enums, and no Markdown or extra text.
- The remote structure contains no free-text answer. After strict claims and intent validation, Python renders the Chinese response from Java-owned facts.

## Offline verification

Python uses `httpx.MockTransport`; Java uses a localhost stub. Tests cover field-level configuration fallback, timeout-code separation, payload shape, strict schema, intent mismatch rejection, fact-bound rendering, fallback behavior, and local slow-response handling. No endpoint, credential, environment value, raw upstream response, or local path is recorded here.

Latest offline regression: Python 37 passed (one existing framework deprecation warning); Java 63 passed with 5 conditional MySQL reliability tests skipped. Provider-related host environment variables were removed from the test process before the Java regression.

## AI1.3 final synthetic smoke

After the offline gates, one fresh end-to-end synthetic SKU-code request was authorized and sent through the public Java entry point. The final result is `AI_PRODUCT_ASSISTANT_REAL_SMOKE_PASS`: the compatible-provider path completed, strict response and claim validation passed, local intent matched `SKU_CODE`, the grounded renderer returned the Java-owned SKU fact, Java Evidence and Trace were persisted, and fallback was not used. No retry was performed. This is a bounded integration result, not a production availability or capacity claim.
