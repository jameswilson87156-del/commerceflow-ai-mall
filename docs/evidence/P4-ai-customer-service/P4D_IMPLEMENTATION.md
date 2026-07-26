# P4D Implementation

Run date: 2026-07-26

## Java fallback classifier

`AiService` now uses a deterministic, local classifier only after the Java-to-Python call fails, times out, returns an invalid response, or returns a provider error status.

| Question category | Local fallback result |
| --- | --- |
| `PRICE` | Current Java-owned sale price for the selected Product and SKU. |
| `SPECIFICATION` | Current color, size, and SKU code. |
| `SKU_CODE` | Current SKU code. |
| `STOCK_OR_PURCHASE` | Product and SKU sale status first, then current available stock. |
| `UNSUPPORTED` | `UNSUPPORTED_QUESTION`; no stock or price answer is generated. |

Unsupported terms include shipping, logistics, refunds, payment, discounts, orders, other-user access, price or inventory changes, system-prompt requests, rule-bypass language, and script or JavaScript injection language.

Every Java fallback response uses `java-fact-fallback` with provider mode `FALLBACK` and `fallbackUsed=true`. Unsupported fallback also returns a warning that the AI service is unavailable and that the question is outside the local product-facts support scope.

## Trace timing

The existing six steps are still returned in this order:

1. `REQUEST_RECEIVED`: request validation.
2. `BUSINESS_FACTS_LOADED`: Product, SKU, and Inventory lookup plus `businessFacts` construction.
3. `PYTHON_REQUEST_SENT`: construction of the constrained Java-to-Python request.
4. `PROVIDER_COMPLETED`: actual Java-to-Python wait time, including time until a timeout or unavailable-service failure.
5. `RESPONSE_VALIDATED`: provider-response validation or fallback-result construction.
6. `RESPONSE_RETURNED`: Evidence and response assembly, ready for return.

`RESPONSE_RETURNED` no longer claims that the browser has already received the answer. It represents an assembled response that is ready to return.

## Frontend

The Vue workbench consumes `answerStatus`, `fallbackUsed`, `warning`, and each backend `durationMs` without creating a frontend-only status or replacing elapsed values. The right-side Evidence and Trace typography was increased slightly, while section spacing was tightened so all six real Trace steps remain visible in the final desktop viewport.
