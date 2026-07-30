# Provider failure observability hardening

## Scope

AI1.1 added secret-safe failure provenance to the existing Java-to-FastAPI-to-provider boundary. The initial real smoke result `AI_PRODUCT_ASSISTANT_REAL_SMOKE_FAILED` remains historical evidence and is not rewritten. After the AI1.1 and AI1.2 fixes, the separately authorized AI1.3 synthetic smoke completed with `AI_PRODUCT_ASSISTANT_REAL_SMOKE_PASS`.

## Safe sources

- `JAVA_TO_FASTAPI`: Java could not complete the local FastAPI boundary.
- `REMOTE_PROVIDER`: FastAPI reached its remote-provider adapter but the provider transport or HTTP status failed.
- `PROVIDER_RESPONSE_VALIDATION`: a response existed but did not satisfy the required protocol or fact-validation boundary.
- `PROVIDER_CONFIGURATION`: provider mode, protocol, or required configuration was rejected before a remote request.

Only a stable safe code is placed in the Trace detail and `ai_trace.error_code`. No exception body, endpoint, credential, authorization header, model value, raw provider payload, or local path is stored.

## Local verification

- Java local HTTP stubs distinguish Java-to-FastAPI unavailable from typed FastAPI failures.
- Python `httpx.MockTransport` verifies provider HTTP, transport, JSON, schema, and fact-validation classes.
- Java fallback continues to return `java-fact-fallback` when enabled. Disabling fallback returns a service error without a local answer.

The original real smoke lacked a per-request source marker. AI1.1 added diagnostic categories only and made no new real provider request. AI1.3 later performed exactly one separately authorized synthetic request, with no retry; its pass evidence is recorded separately. The historical failure record is retained for auditability.

## AI1.2 readiness boundary

The Python remote read budget is bounded at 10 seconds and Java waits 15 seconds for the local FastAPI result, retaining a 5-second processing margin. Local Java-to-FastAPI timeout and remote-provider timeout retain distinct safe codes. The AI1.3 smoke used the minimum Chat Completions payload (`model` and `messages`) and one bounded request without automatic retries.

## AI1.4 URL boundary

Remote URL validation is fail-closed: HTTPS is required for non-local hosts; plain HTTP is allowed only for `localhost`, `127.0.0.1`, and `::1` test targets. Userinfo, query strings, and fragments are rejected. Base URL path variants are normalized to `/v1/chat/completions`, and configuration errors expose neither the URL nor credential material.
