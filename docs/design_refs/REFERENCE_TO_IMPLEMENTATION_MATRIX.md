# Reference to Implementation Matrix

This matrix turns each AI reference into an implementation contract. It is intentionally conservative: a layout may be retained even when its pictured values or controls cannot.

## 01 Dashboard Reference

| Item | Registration |
| --- | --- |
| File / target / status | `01-dashboard-reference.png` / Vue admin / `APPROVED_V1_REFERENCE` |
| Corresponding real page | Vue Overview |
| Corresponding real API | `GET /api/products`; `GET /api/orders?userId=1` |
| Layout that may remain | Dark sidebar, single topbar, compact summary cards, catalog health list, recent-order table, compact AI entry point. |
| Replace with real data | Product count or SKU count, available-stock sum, product rows, recent orders, order amount/currency/status/time. |
| Remove as non-existent | Marketing, users, shipping, payment, notifications, global search, activity feed, daily/pending KPIs, AI completion rate. |
| Frontend additions | Explicit real-data labels, loading/empty/error states, local capture timestamp, narrow AI evidence callout. |
| Backend additions | None for a reduced dashboard. A summary/date-window API is required before any KPI beyond counts; trends need a separate aggregation API. |
| Implementation risk | High if implemented first: dashboard encourages fake aggregates before source pages are real. |
| Final real screenshot | `screenshots/v2/01-dashboard-real.png` |

## 02 Product and SKU Reference

| Item | Registration |
| --- | --- |
| File / target / status | `02-product-sku-reference-v1.png` / Vue admin / `APPROVED_V1_REFERENCE` |
| Corresponding real page | Vue Catalog |
| Corresponding real API | `GET /api/products`; `GET /api/products/{productId}` |
| Layout that may remain | Product list, selected-product detail, SKU table, price/stock emphasis, read-only informational footer. |
| Replace with real data | Product ID/name/description/category, SKU code, color, size, sale price, currency, `availableStock`, count labels. |
| Remove as non-existent | Product images unless an asset source is added, fake product/SKU counts, fake stock values, create/edit/bulk operations, unimplemented filtering. |
| Frontend additions | Selected-product state, SKU subtable, documented stock threshold badge, Chinese empty/loading/error state, read-only wording. |
| Backend additions | None for baseline read-only view. Search/filter/pagination or product writes require real query/write contracts. |
| Implementation risk | Medium: visual density can hide that the current API only returns two products and three SKUs. |
| Final real screenshot | `screenshots/v2/02-product-sku-real.png` |

## 03 Order and Inventory Reference

| Item | Registration |
| --- | --- |
| File / target / status | `03-order-inventory-reference-v1.png` / Vue admin / `APPROVED_V1_REFERENCE` |
| Corresponding real page | Vue Orders |
| Corresponding real API | `POST /api/orders?userId=1`; `GET /api/orders?userId=1`; `GET /api/orders/{orderNo}` |
| Layout that may remain | Order master-detail layout, order snapshot table, status summary, transaction explanation area. |
| Replace with real data | Order number, amount, currency, `CREATED`, created time, OrderItem product/SKU/attributes/unit price/quantity snapshot. |
| Remove as non-existent | Fake user rows, payment/shipping states, first-create/replay/conflict as list rows, fake inventory values, static success/failure history. |
| Frontend additions | Detail selection, Chinese status mapping, request-result card, collapsible development evidence, real empty/error states. |
| Backend additions | `GET` inventory-change evidence keyed by order/SKU if before/deducted/after values must be presented. |
| Implementation risk | High: idempotency semantics are easy to misrepresent; no list row is created for replay or 409 conflict. |
| Final real screenshot | `screenshots/v2/03-order-inventory-real.png` |

## 04 AI Customer Service Reference

| Item | Registration |
| --- | --- |
| File / target / status | `04-ai-customer-service-reference-v1.png` / Vue admin / `APPROVED_V1_REFERENCE` |
| Corresponding real page | Vue AI Support |
| Corresponding real API | `POST /api/ai/product-chat`; Python `POST /v1/product-answer`; Python `GET /health` |
| Layout that may remain | Facts / question-answer / evidence three-column workbench, explicit Mock badge, fallback boundary section, one-way call-chain illustration. |
| Replace with real data | Question, answer, trace ID, provider mode, status, evidence tag; product/SKU/stock facts only when exposed by an actual response. |
| Remove as non-existent | Static availability statement, fake elapsed time, fake trace history, fabricated provider/model details, stored exception examples presented as incidents. |
| Frontend additions | Dynamic service-state handling, response/fallback state, compact fact rendering, Chinese empty/loading/error state. |
| Backend additions | A Java-owned AI status endpoint if the Vue page must show availability. Trace history/detail remains deliberately out of scope until a read contract exists. |
| Implementation risk | Medium-high: facts must stay Java-authoritative and Python must never appear to own stock or write business data. |
| Final real screenshot | `screenshots/v2/04-ai-customer-service-real.png` |

## 07 Mobile Product Detail Reference

| Item | Registration |
| --- | --- |
| File / target / status | `07-mobile-product-detail-reference-v1.png` / UniApp / `APPROVED_V1_REFERENCE` |
| Corresponding real page | UniApp product sheet, planned stable product-detail screen |
| Corresponding real API | `GET /api/products`; preferred `GET /api/products/{productId}` |
| Layout that may remain | Product identity block, variant selectors, selected-SKU summary, stock/price panel, fixed bottom actions. |
| Replace with real data | Name, description, SKU code, color, size, sale price, currency, current `availableStock`. |
| Remove as non-existent | Image variations without an asset source, material/scene/style rows, share flow, review/recommendation/delivery claims, unavailable color/size combinations. |
| Frontend additions | Stable detail route, selected-SKU state, disabled zero-stock state, collapsed option list, safe-area layout, Chinese statuses. |
| Backend additions | None for basic detail; the existing detail endpoint should be used. Image metadata requires a later asset contract. |
| Implementation risk | Medium: selected state, stock state, and bottom action wording must remain synchronized. |
| Final real screenshot | `screenshots/v2/07-mobile-product-detail-real.png` |

## 08 Mobile Order Flow Reference

| Item | Registration |
| --- | --- |
| File / target / status | `08-mobile-order-flow-reference-v1.png` / UniApp / `APPROVED_V1_REFERENCE` |
| Corresponding real page | UniApp Bag and checkout, planned confirmation/result screens |
| Corresponding real API | `GET/POST /api/cart?userId=1`; `POST /api/orders?userId=1`; `GET /api/orders/{orderNo}` |
| Layout that may remain | Cart -> confirmation -> result sequence, real order-total summary, result card, optional expandable developer evidence. |
| Replace with real data | Demo buyer identity mapping, product/SKU snapshots, quantity, amount, currency, order number, `CREATED`, current stock only when fetched. |
| Remove as non-existent | Payment success, shipping/ETA, coupon/points, a usable View Order button before detail exists, technical jargon in normal buyer content. |
| Frontend additions | Persistent cart integration, confirmation screen, order-result screen, detail navigation, retry/failure states, development-evidence disclosure. |
| Backend additions | No new endpoint for basic submit/result: cart and order APIs already exist. An explicit UI identity mapping is needed if `U10001` is shown. |
| Implementation risk | High: an order result can be honest only when it says `CREATED`, not paid or shipped; replay should not create a second result. |
| Final real screenshot | `screenshots/v2/08-mobile-order-flow-real.png` |

## Deliberately Missing References

| Page | Current decision | Required future API |
| --- | --- | --- |
| 05 AI Product Copy | Do not generate or stage a runtime reference | Dedicated Java copy endpoint, Python copy contract, validation, trace/evidence, tests |
| 06 Trace Detail | Do not generate or stage a runtime reference | Java trace read endpoint with privacy-safe parsed evidence and tests |

