# P4C Implementation

## Scope

P4C adds a real, read-only Vue workbench for the existing AI product customer-service chain. It is a showcase interface, not a hosted AI product and not a general chat system.

The delivered path is:

```text
Vue product and SKU selection
  -> POST /api/ai/customer-service/ask
  -> Java loads Product, SKU, and Inventory facts from MySQL
  -> Java calls local Python commerceflow-mock
  -> Java returns its own Evidence and Trace
  -> Vue renders the structured response as plain text
```

The browser never calls Python directly. The page does not send price, stock, product status, or any other business fact in the public request.

## Frontend Changes

| File | Responsibility |
| --- | --- |
| `apps/admin-web/src/AiCustomerServiceWorkbench.vue` | Three-column customer-service workbench, browser-session messages, real states, Evidence and Trace display. |
| `apps/admin-web/src/aiCustomerService.ts` | Exact public API contract, real product loading, local filters, and bounded `clientRequestId` generation. |
| `apps/admin-web/src/aiCustomerService.spec.ts` | Contract-level component and helper coverage. |
| `apps/admin-web/src/App.vue` | Existing navigation-style page switch to the real AI customer-service workbench. |
| `apps/admin-web/src/App.spec.ts` | Navigation regression coverage. |
| `apps/admin-web/src/style.css` | P4C-scoped visual rules only. |

The default selection is real API result `skuId=10004` when it exists; otherwise the first API-returned SKU is selected. It is not a frontend fixture. The selected demo SKU is the gray L T-shirt: `T-SHIRT-GRAY-L`, `¥129.00 CNY`, available stock `28` in the validated local data.

## User-Facing Behavior

- Left panel: real product/SKU cards from `GET /api/products`, product-name/SKU-code search, sale-state filter, inventory-state filter, images from API image paths, and image-failure placeholder.
- Center panel: browser-session-only single-turn messages, five quick questions, Chinese input, whitespace trimming, `500` character maximum, Enter send, Shift+Enter newline, duplicate-send prevention, and retry after a request error.
- Right panel: selected-SKU preview before a question; after a response, the Java-returned `businessFacts` are the explicit source for that answer. Evidence is grouped only by `PRODUCT`, `SKU`, and `INVENTORY`; Trace uses the six actual backend steps.
- Provider card: dynamic response values. Before a response it says that the first request is pending. A normal local response visibly states `commerceflow-mock`, `MOCK`, no external network, no real API key, real `latencyMs`, and fallback status.

## Boundaries Preserved

- No Java, Python, Flyway, MySQL schema, or provider behavior changed in P4C.
- No P2 product page or P3 order page component was modified.
- No real API key, external AI model, external network model call, order write, inventory write, payment, shipping, refund, or chat-history persistence was introduced.
- The reference image is not used as a page background or runtime asset.
- Responses and user text are rendered with Vue text interpolation only; no `v-html`, Markdown-to-HTML, script evaluation, raw SQL, raw prompt, stack trace, authorization value, or provider configuration is displayed.

## Real Implementation Fix

After the first real response, the message card updated but the right-side facts/Evidence/Trace panel initially stayed on its selection preview. The root cause was a mutation of an object that had already been pushed into a Vue ref array. The completion path now replaces the affected message through an immutable array update, allowing `latestResponse` and all derived panels to recompute. This was a frontend reactivity defect; it did not require a backend change.

