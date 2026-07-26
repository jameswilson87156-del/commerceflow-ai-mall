# P4 AI Customer Service Current Baseline

**Audit date:** 2026-07-26

**Document status:** Historical P4A audit snapshot. It is intentionally retained as the pre-implementation baseline; P4B implementation evidence is recorded in the `P4B_*.md` files in this directory.

**Audited commit:** `a21d4057be3ed8b9b99026d44b01ecf6cd63dc33`
**Scope:** Read-only audit. No P4 implementation is included in this document.

## What Exists Now

| Area | Confirmed current implementation | Assessment |
| --- | --- | --- |
| Java entry | `POST /api/ai/product-chat` accepts only `question` and `userId`. | Prototype only; it cannot select a real Product or SKU. |
| Java fact loading | `AiService` calls `CatalogRepository`, but always loads SKU `10001`. | Java owns the lookup, but the chosen SKU is hard-coded. |
| Java to Python | Java makes a one-way HTTP `POST` to `http://127.0.0.1:8000/v1/product-answer`, with 2s connect and 4s read timeouts. | A real one-way call exists; Python does not call Java back. |
| Java response parsing | The request JSON is hand-built and the response is read with string extraction. | Not safe enough for P4's structured contract or invalid JSON handling. |
| Python | FastAPI exposes `/health` and `/v1/product-answer`, Pydantic request/response models, deterministic Mock mode, and an optional OpenAI-compatible branch. | Usable prototype, but English-only mock answers and weak provider-output validation remain. |
| Python data access | No database driver, MySQL configuration, or Java callback exists. | Correct P4 boundary already exists. |
| Trace | Java writes one `ai_trace` row with trace id, full question, mode, answer, string evidence JSON, and status. | A basic persisted trace exists; it lacks request correlation, product/SKU ids, latency, fallback flag, safe question summary, and explicit answer status. |
| Product facts | Product, SKU, current `available_stock`, price, currency, status, and local image paths are available through MySQL and Java catalog queries. | Sufficient for the narrow P4 question set. Current inventory is `NOT NULL`; its value can be zero. |
| Vue | The sidebar contains a non-interactive `AI 客服` item. No AI page, API module, TypeScript contract, product selection, messages, loading/error/retry, trace, or evidence view exists. | P4 UI is not implemented. |
| Tests | Java has catalog/order tests but no AI test. Python has two generic FastAPI tests. Vue has catalog/order tests but no AI test. | No P4 acceptance coverage exists. |
| Infrastructure | MySQL Docker Compose has a health check. Java port is configurable. AI port/key/mode variables exist in `.env.example`; CI runs Python tests. | Mock configuration exists, but no P4 all-services startup or AI-specific CI acceptance exists. |

## Running and Non-running Chains

### Can run today

`Java /api/ai/product-chat -> fixed SKU 10001 -> Python /v1/product-answer -> Java ai_trace write -> Java response` can run when both services are started. The default Python Mock branch is deterministic and does not need an API key.

### Cannot honestly be presented as P4-ready

- A user cannot select Product 101/102 or a SKU in the Java request or Vue page.
- The Java fact payload is not a typed, validated contract and always declares `productStatus` as `ON_SALE`.
- Java discards Python structured fields, evidence, risk, latency, and provider failures; it returns only a simplified answer/mode/status.
- Java's fallback answer is hard-coded English copy, not a product-specific structured fallback.
- The optional real-provider branch falls back to Mock when credentials are absent, which could be mistaken for a real-provider result.
- No Vue AI workbench or real local runtime screenshot exists.
- No Java-owned Evidence model or trace-read API exists.

## Integrity Findings

- **Hard-coded response/fact selection:** Yes. Java always uses SKU `10001`; fallback wording is also hard-coded.
- **Frontend fake data:** No AI page exists, so no AI frontend mock data exists. The current navigation label alone is not a functioning entry.
- **Real Java-to-Python call:** Yes, one way only.
- **Real business-fact injection:** Partial. Java queries true catalog facts, but for a fixed SKU rather than the user-selected SKU.
- **Trace/Evidence:** Basic `ai_trace` persistence exists; Java stores only `["java.businessFacts"]`, not displayable field evidence or trace steps.

## Primary P4 Gaps

1. Typed external and internal contracts with strict validation.
2. SKU selection, Product/SKU relation validation, and safe fact loading.
3. A deterministic Chinese Mock provider that classifies supported and unsupported questions without claiming to be a hosted model.
4. Java-generated Evidence and Java validation of Python's structured response.
5. A privacy-minimized extension of the existing trace table and a P4 trace-summary response.
6. A real Vue customer-service workbench and complete tests.
