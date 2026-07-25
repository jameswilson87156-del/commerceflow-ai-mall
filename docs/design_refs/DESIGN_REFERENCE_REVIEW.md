# CommerceFlow AI Mall Design Reference Review

## Registration Status

All six files in `docs/design_refs/generated/` are registered as `APPROVED_V1_REFERENCE`. This status approves their use as V1 visual direction only. It does not approve any pictured KPI, data row, state, or workflow as implemented.

These files are AI-generated references, not runtime evidence. They must not appear in the root README real-screenshot area, test reports, build evidence, or interview claims of completed functionality. Only browser captures from a running page, connected to the local API and captured after the relevant test/build gate, may be stored under `screenshots/v2/`.

`05 AI Product Copy` and `06 Trace Detail` are intentionally absent from this set. Their missing APIs are a scope boundary, not an omission.

| No. | File | Page | Target | Status | Current real page | Current real API | Planned real screenshot |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 01 | `01-dashboard-reference.png` | Operations overview | Vue admin | `APPROVED_V1_REFERENCE` | Vue Overview | `GET /api/products`, `GET /api/orders?userId=1` | `screenshots/v2/01-dashboard-real.png` |
| 02 | `02-product-sku-reference-v1.png` | Product and SKU management | Vue admin | `APPROVED_V1_REFERENCE` | Vue Catalog | `GET /api/products`, `GET /api/products/{productId}` | `screenshots/v2/02-product-sku-real.png` |
| 03 | `03-order-inventory-reference-v1.png` | Order management and inventory deduction | Vue admin | `APPROVED_V1_REFERENCE` | Vue Orders | `POST /api/orders?userId=1`, `GET /api/orders?userId=1`, `GET /api/orders/{orderNo}` | `screenshots/v2/03-order-inventory-real.png` |
| 04 | `04-ai-customer-service-reference-v1.png` | AI product support workbench | Vue admin | `APPROVED_V1_REFERENCE` | Vue AI Support | `POST /api/ai/product-chat`, Python `POST /v1/product-answer`, Python `GET /health` | `screenshots/v2/04-ai-customer-service-real.png` |
| 07 | `07-mobile-product-detail-reference-v1.png` | Product detail and SKU selection | UniApp | `APPROVED_V1_REFERENCE` | UniApp product sheet | `GET /api/products`, `GET /api/products/{productId}` | `screenshots/v2/07-mobile-product-detail-real.png` |
| 08 | `08-mobile-order-flow-reference-v1.png` | Cart, confirmation, and order result | UniApp | `APPROVED_V1_REFERENCE` | UniApp Bag and checkout | `GET/POST /api/cart`, `POST /api/orders?userId=1`, `GET /api/orders/{orderNo}` | `screenshots/v2/08-mobile-order-flow-real.png` |

## Per-image Review

### 01 Operations Overview

The dark navigation shell, compact card rhythm, product/stock table shape, recent-order region, and AI support entry are worth preserving. Counts must be derived from current product and order responses. The figure's daily order KPI, shipment KPI, order trend, AI completion rate, notifications, marketing, user management, logistics, payment, and activity feed must not be replicated as working UI. This page needs a later V2 reference or a real-page replacement because a dashboard is downstream of the other page data.

### 02 Product and SKU Management

The master-detail composition, selected product summary, and SKU grid are a strong fit for the existing product response. Product count, SKU count, SKU codes, prices, and stock values shown in the image are not Flyway facts and must be replaced. Search, filter, paging, create, edit, and bulk action controls remain absent until implemented. The current page is read-only; its visual design must communicate that honestly.

### 03 Order Management and Inventory Deduction

The order list plus detail layout, snapshot table, and transaction explanation can be retained. The reference incorrectly treats first creation, idempotent replay, and key conflict as three order-list rows. They are request-result outcomes: replay returns the original order and key conflict returns HTTP 409 without creating a row. Inventory must use a before / current deduction / after presentation only when an audit API returns those facts. Use Chinese presentation status as primary text and `CREATED` as the code status. No payment, shipping, or fulfillment state may be introduced.

### 04 AI Product Support Workbench

The three-column facts-question-answer structure and explicit Java-owned facts boundary can be retained. `providerMode`, `status`, `traceId`, and business facts must come from a real response or a future trace-read response; the availability chip cannot be static. The exception cards are boundary illustrations, not a record of observed production faults. Do not add a trace-history panel while the project has only trace write capability.

### 07 Mobile Product Detail and SKU Selection

The SKU picker, price/stock summary, and fixed bottom action area are appropriate. When Black/M is selected, the page must not still say that a specification is required. The product image must change per SKU or be labelled as a common product image; the database currently has no image asset field. The selectable SKU list may collapse to control height. Any zero-stock SKU must disable purchase actions. Current data supports only the real product/SKU fields, not material, scene, style, review, delivery, or recommendation fields.

### 08 Mobile Cart, Confirmation, and Result

The three-step sequence is suitable once it is driven by the existing order API. Use a single user-display identifier of `U10001` in the reference without trailing characters; runtime implementation must map it consistently from the current demo identity rather than manufacture a second user. Keep atomic update and HTTP 409 explanations inside an expandable `Development demo information` area, not in the normal buyer journey. `View order` is conditional on a real order-detail screen. `Order created successfully` means only `CREATED`; it never implies payment or shipping.

## Real Screenshot Gate

Before creating any file under `screenshots/v2/`, the corresponding page must satisfy all of these conditions:

1. It is a real running Vue or UniApp page, not a reference image placed in a browser surface.
2. It reads from the local API or displays a result produced by a real local operation.
3. Values originate from Flyway seed data or documented runtime actions.
4. The UI is Chinese for the new V2 capture.
5. It contains no fabricated aggregate, payment, logistics, provider-health, or trace-history claims.
6. The page-specific build and tests have passed before capture.

