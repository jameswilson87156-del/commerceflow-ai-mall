# P4E Implementation

## Goal

Freeze the AI customer-service Showcase at a real `1920 x 1080` desktop layout. The first viewport must show the sidebar, five SKU choices, the chat question and answer, Provider metadata, business facts, seven Evidence entries, and six Trace steps together.

## Changed Frontend Surface

- `apps/admin-web/src/AiCustomerServiceWorkbench.vue`
  - Adds stable test hooks for the three work areas.
  - Gives long Trace IDs, Evidence fields, and Trace detail rows accessible full-value titles while allowing only their local visual fields to truncate.
- `apps/admin-web/src/style.css`
  - Adjusts the three-column sizing, panel spacing, SKU-card density, composer density, fact/evidence density, and Trace line clamping.
- `apps/admin-web/src/aiCustomerService.spec.ts`
  - Adds coverage for the three mounted work areas, a long technical Trace ID, and a long user question.

## Unchanged Boundaries

- The page is still read-only for Product, SKU, Inventory, Evidence, and Trace.
- The page still reads the local Java product and AI endpoints. No frontend business facts, stock, Provider result, Evidence, or Trace are fabricated.
- The normal screenshot uses `commerceflow-mock / MOCK`; the fallback screenshot uses `java-fact-fallback / FALLBACK` after the local Python service is deliberately stopped.
- No payment, delivery, logistics, refund, order write, Redis, MyBatis, object storage, message queue, or new AI capability was introduced.

## Final Freeze

P4 visual work is frozen after the P4E acceptance commit. Future P4 visual or database changes require a reproducible real defect; ordinary visual preference changes are out of scope.
