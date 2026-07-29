# P7 Operations Overview Specification

## Decision

P7B **may implement** one read-only operations overview only if it is built from real local APIs. It is not needed to prove the existing P2-P6 flows, but it is worthwhile as a final admin entry point after its facts can be computed honestly. Until then, the current sidebar label is not a dashboard and must not be used in a release screenshot.

## Allowed Metrics

| Display | Source of truth | Minimal read model |
| --- | --- | --- |
| On-sale product count | `product.status='ON_SALE'` | Catalog summary query. |
| SKU count | `product_sku` joined to on-sale products | Catalog summary query. |
| In-stock / low-stock / out-of-stock SKU counts | `inventory.available_stock`, using the same published UI threshold | Catalog summary query. |
| Created order count | `orders.status='CREATED'` | Order summary query. |
| AI request result counts by provider/fallback over a bounded local interval | `ai_trace` rows only | Trace summary query, explicitly labelled local Showcase data. |
| Redis limiter availability | Existing readiness/rate-limit state | Current runtime status only, never a fabricated percentage. |

## Disallowed Metrics

- GMV, conversion, sales trend, forecast, DAU, success rate, growth percentage, shipping queue, payment state, user management, marketing center, notification center, or any trend without persisted source data.
- A chart whose values are prefilled in Vue or whose date range has no documented query.
- Analysis tables or a new Flyway migration merely to make KPIs look richer.

## API And Persistence Decision

P7B should add the smallest read-only endpoint only after agreeing its JSON contract. Reuse `JdbcTemplate` for small aggregate queries consistent with current catalog/order repositories. Do not add MyBatis, a new analytics table, or a Flyway migration unless a required metric cannot be truthfully obtained from current tables.

## 1920x1080 Layout

- Keep the existing dark sidebar and clearly label the page `本地 Showcase 概览`.
- Top: five compact fact cards, each with source/date context rather than invented deltas.
- Middle left: inventory distribution table; middle right: order and AI local-runtime status summaries.
- Bottom: explicit boundaries and links to product, order evidence, and AI workbench.
- Loading, empty, and error must be real API states. No charts are required.

## Acceptance

Every visible number must be reproducible from the response shown in browser network tools. The canonical screenshot must state local Showcase data and avoid implying operating production commerce.
