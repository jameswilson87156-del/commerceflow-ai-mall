# UI Implementation Order

## Required Sequence

1. Product and SKU management
2. Order management and inventory evidence
3. AI product support
4. UniApp product detail
5. UniApp cart and checkout
6. Operations overview last

The overview is deliberately last because it depends on trustworthy source-page data and, if enhanced, aggregation APIs. Building it first would make it too easy to manufacture KPI cards that have no source semantics.

## Page Gates

| Order | Page | Why now | API work | Capture gate |
| ---: | --- | --- | --- | --- |
| 1 | Product and SKU management | Establishes the source-of-truth visual language and real catalog fields used elsewhere. | None for read-only baseline; use product detail API. | Real Chinese product and SKU rows, no fake controls, build passes. |
| 2 | Order management and inventory evidence | Proves the transaction and snapshot story using the actual catalog. | Add inventory-evidence read API only if before/deducted/after is shown. | A newly submitted order detail, `CREATED` code status, and no replay/conflict rows. |
| 3 | AI product support | Depends on real product/SKU/stock facts and clear evidence language. | Java AI status endpoint only if availability is shown; no trace-history API in this phase. | Mock response from live API, returned trace ID/status/evidence, fallback state handled. |
| 4 | UniApp product detail | Reuses actual product and SKU data after the desktop source page is solid. | No new API; use `GET /api/products/{productId}`. | Selected SKU, disabled zero-stock option, safe-area layout, Chinese UI. |
| 5 | UniApp cart and checkout | Builds on a stable selection and uses the real transaction contract. | Existing cart/order APIs; no new endpoint for a basic success view. | `CREATED` result with actual order number; developer details collapsed; no payment/shipping claim. |
| 6 | Operations overview | Becomes a summary of proven pages rather than a promise of future data. | Optional dashboard summary/aggregation API only after KPI definitions are approved. | Every card has a documented API source and no fabricated trend/completion/shipment metrics. |

## Implementation Discipline

- Change one page per implementation cycle.
- Write the page specification and API mapping before editing the page.
- Run the relevant build and tests before capture.
- Capture one real page at a time; perform the visual and data review before moving on.
- Store only approved real captures under `screenshots/v2/` using the registered names.
- Keep AI references in `docs/design_refs/generated/`; never use one as a browser background or screenshot substitute.

## V2 Screenshot Names

```text
screenshots/v2/
  01-dashboard-real.png
  02-product-sku-real.png
  03-order-inventory-real.png
  04-ai-customer-service-real.png
  07-mobile-product-detail-real.png
  08-mobile-order-flow-real.png
```

