# P4B Grounded AI Customer Service Backend

## Scope

P4B replaces the P4A prototype backend. It does not create the Vue AI workbench or a P4 screenshot; those remain P4C work.

## Implemented Chain

1. `POST /api/ai/customer-service/ask` validates positive `userId`, `productId`, `skuId`, a trimmed 1-500-character question, and a bounded `clientRequestId`.
2. Java verifies Product/SKU ownership and reads Product, SKU, Inventory, image path, statuses, and `BigDecimal` price from MySQL.
3. Java creates typed `businessFacts`, with `unitPrice` held as `BigDecimal` and serialized to Python as a decimal string.
4. A bounded Spring `RestClient` sends the typed request to `POST /internal/ai/customer-service/answer`.
5. FastAPI/Pydantic validates the strict request. `CommerceFlowMockProvider` returns deterministic Chinese text from those facts only.
6. Java validates the typed response, generates Evidence from its own facts, writes a privacy-minimized Trace summary, and returns the final DTO.

## Replaced Prototype Behavior

- Removed the fixed `SKU 10001` selection, hand-built JSON, and substring response parsing.
- Removed the Python path that silently presented Mock output as a failed real-provider result.
- The former `/api/ai/product-chat` route remains only as a deprecation response; it cannot select a SKU and no longer runs a competing core flow.
- Python no longer exposes the prototype `/v1/product-answer` endpoint, has no database dependency, and does not return Evidence, facts, or Trace data.

## Contract Resolution

P4A used `salePrice` in a draft example while P4B required `unitPrice`. The resolved contract is `unitPrice`: Java uses `BigDecimal`; JSON uses its exact decimal string. P4A also described an optional `productId`; P4B requires it and validates Product/SKU matching before any Python call. The design contracts record both decisions.

## Current Boundary

This is a local Showcase backend. It supports one selected SKU and a narrow fact-bound question set. It does not implement a browser AI page, real hosted provider, conversation history, Trace read endpoint, RAG, order lookup, payments, logistics, refunds, tools, or writes beyond the `ai_trace` summary.
