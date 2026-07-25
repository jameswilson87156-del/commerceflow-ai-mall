# Reference Image Sequence

The sequence below plans eight design references while preserving a hard distinction between layout intent and current runtime support.

## 01 Operations Overview

| Item | Baseline |
| --- | --- |
| Page goal | Let a reviewer understand catalog health, demo order activity, and AI evidence boundaries in one desktop view. |
| Already real | Product/SKU count from `GET /api/products`, stock sum from nested `availableStock`, order list from `GET /api/orders?userId=1`, AI support navigation. |
| Real APIs | `GET /api/products`; `GET /api/orders?userId=1`. |
| Real fields | Product count, SKU count, available stock, order number, amount, currency, status, created time. |
| Real demo data | Two products, three seeded SKUs, demo user, runtime-created `CREATED` orders. |
| Frontend additions | Explicit metric labels, empty states, capture timestamp, API loading/error states, and a compact recent-order table. |
| Backend additions | Only required for date-window metrics, order trends, pending work, or activity feed. Current reference should not need them. |
| Prohibited fiction | Today's KPI, pending shipment, trend line, latest activity, AI completion rate, payment, logistics, marketing, or user-management counts. |
| Recommended layout | Keep the dark sidebar and four-card rhythm only if each card is real; use a catalog table and recent orders below; keep AI as a small evidence callout. |
| Recommended size | 1920 x 1080 desktop; capture a 1440 x 900 fallback if the browser viewport cannot fit the full layout. |

## 02 Product and SKU Management

| Item | Baseline |
| --- | --- |
| Page goal | Inspect the real product catalog and variant-level stock without implying write capability. |
| Already real | Vue Catalog view and product API. |
| Real APIs | `GET /api/products`; optional `GET /api/products/{productId}`. |
| Real fields | Product ID, name, description, category name, SKU ID, SKU code, color, size, sale price, currency, available stock. |
| Real demo data | `Essential Cotton Shirt`, `Structured Work Tote`, `SHIRT-BLK-M`, `SHIRT-WHT-L`, `TOTE-TAN-ONE`. |
| Frontend additions | SKU subtable, stock badge derived from a documented threshold, product detail drawer, loading/error/empty states, read-only label. |
| Backend additions | Product/SKU/category CRUD, filters, pagination, or stock history only if the image requires them. |
| Prohibited fiction | Edit buttons that submit nowhere, sales in 30 days, reviews, ratings, product photos, supplier data, warehouse location, or replenishment suggestions. |
| Recommended layout | Product summary rows on the left, selected product SKU table on the right; reserve blue for actual selection and teal for healthy stock. |
| Recommended size | 1600 x 1000 desktop; 1280 x 800 acceptable for a dense catalog table. |

## 03 Order Detail and Inventory Deduction

| Item | Baseline |
| --- | --- |
| Page goal | Explain one successful checkout and show the order/item snapshots that were saved. |
| Already real | Order submit, idempotency, guarded inventory update, order list/detail APIs, transaction tests. |
| Real APIs | `POST /api/orders?userId=1`; `GET /api/orders/{orderNo}`; `GET /api/orders?userId=1`. |
| Real fields | Idempotency-Key behavior, order number, `CREATED`, total amount, currency, created time, product/SKU/attribute/unit-price/quantity snapshots. |
| Real demo data | One line such as `SHIRT-BLK-M`, quantity 1, CNY 129.00; current stock is read at capture time. |
| Frontend additions | Order detail view, request/result summary, idempotency replay note, inventory outcome message, transaction failure state. |
| Backend additions | Inventory change read endpoint and before/after values if an audit panel is required; no new endpoint is needed for the current order snapshot. |
| Prohibited fiction | Paid, shipped, delivered, refund, carrier, payment transaction, or an exact stock-before value not read from an API. |
| Recommended layout | Left: order header and items; right: transaction facts and guarded decrement explanation; bottom: honest error/replay states. |
| Recommended size | 1440 x 960 desktop; 390 x 844 mobile success state if paired with image 08. |

## 04 AI Product Support Workspace

| Item | Baseline |
| --- | --- |
| Page goal | Show one product question answered from Java-owned catalog and inventory facts. |
| Already real | Java business-facts construction, one-way call to FastAPI, Mock Provider, fallback, trace write. |
| Real APIs | `POST /api/ai/product-chat`; Python `POST /v1/product-answer`; `GET /health` for service state. |
| Real fields | Question, answer, trace ID, provider mode, status, evidence tag; internally: product/SKU identity, color, size, stock, string price, currency, queried time. |
| Real demo data | `Essential Cotton Shirt`, `SHIRT-BLK-M`, Black, M, current stock, CNY 129.00, Mock Provider. |
| Frontend additions | Explicit Mock badge, question/answer timeline, fallback state, evidence summary, no fake online queue. |
| Backend additions | Trace detail/history read API if evidence cards or prior conversations are required. |
| Prohibited fiction | Provider accuracy percentage, human review queue count, invented product attributes, Python database writes, or Java-Python callback loop. |
| Recommended layout | Question composer at top; answer card in the center; compact facts/evidence strip below; keep risk/fallback visibly distinct. |
| Recommended size | 1440 x 960 desktop; 390 x 844 mobile support view only after stable mobile state exists. |

## 05 AI Product Copy Workspace

| Item | Baseline |
| --- | --- |
| Page goal | Future page for generating product copy from selected Java product facts. |
| Already real | Navigation label only; current shell reuses product chat and is not a copy feature. |
| Real APIs | None. |
| Real fields | None beyond product fields that a future contract could accept. |
| Real demo data | Product names/descriptions may be used as input examples, but no generated copy may be presented as real output. |
| Frontend additions | Brief input, source-product panel, structured copy sections, copy provenance, loading/error states. |
| Backend additions | Dedicated Java endpoint and DTO, explicit Python copy contract, output validation, trace/evidence persistence, and tests. |
| Prohibited fiction | A generated description in a formal runtime screenshot, quality score, publishing button, campaign result, or copy history. |
| Recommended layout | Draft workspace with source facts on the left and clearly labelled future output on the right; mark as design-only until the API exists. |
| Recommended size | 1600 x 1000 desktop reference only; no Showcase V1 runtime capture. |

## 06 Trace Evidence Detail

| Item | Baseline |
| --- | --- |
| Page goal | Future read-only inspection of what Java sent, what Python returned, and what risk/evidence labels were recorded. |
| Already real | `ai_trace` insert with trace ID, question, provider mode, answer, evidence JSON, status, and created time. |
| Real APIs | None for reading trace data. |
| Real fields | Database fields exist, but are not currently exposed to a client. |
| Real demo data | A trace ID can be generated by the support call, but must not be shown in a trace page until a read API returns it. |
| Frontend additions | Trace header, request facts, answer, evidence list, risk flags, fallback status, privacy-safe redaction. |
| Backend additions | `GET /api/ai/traces/{traceId}` or equivalent DTO, evidence parsing, authorization boundary, and tests. |
| Prohibited fiction | Reference-image source rows, invented stock timestamps, provider latency, human approval, or trace details manually copied from the image. |
| Recommended layout | Narrow trace header, two-column source/answer comparison, evidence cards, and explicit privacy note. |
| Recommended size | 1440 x 1000 desktop reference only; no Showcase V1 runtime capture yet. |

## 07 UniApp Product Detail and SKU Selection

| Item | Baseline |
| --- | --- |
| Page goal | Let a demo buyer inspect a real product, choose a real SKU, and see current stock and price. |
| Already real | UniApp H5 catalog, local product sheet, SKU selection, local add-to-bag interaction. |
| Real APIs | `GET /api/products`; preferred future detail call `GET /api/products/{productId}`. |
| Real fields | Product name, description, SKU code, color, size, sale price, currency, available stock. |
| Real demo data | Both seeded products and all three seeded SKUs. |
| Frontend additions | Stable detail route/screen, selected-SKU state, disabled unavailable option, safe-area padding, loading/error state. |
| Backend additions | None for read-only current data; a detail endpoint is already available but the page should use it for a stable single-product capture. |
| Prohibited fiction | Product imagery, reviews, ratings, recommendations, delivery promise, or login state not returned by the API. |
| Recommended layout | Product identity first, variant selector second, live stock/price third, primary add action at the bottom. |
| Recommended size | 390 x 844 and 375 x 812; keep content inside top and bottom safe areas. |

## 08 UniApp Bag, Confirmation, and Success

| Item | Baseline |
| --- | --- |
| Page goal | Demonstrate a real order submission from a selected SKU through an honest success result. |
| Already real | Local bag state, order POST, `Idempotency-Key`, order number return, local bag clearing. |
| Real APIs | `POST /api/orders?userId=1`; `GET /api/orders/{orderNo}` for a future success detail; cart APIs exist but are not used by the current page. |
| Real fields | SKU code, product name, quantity, price, order number, total amount, currency, `CREATED`. |
| Real demo data | One selected SKU, typically `SHIRT-BLK-M` at CNY 129.00 or `TOTE-TAN-ONE` at CNY 299.00. |
| Frontend additions | Confirmation screen, success screen with order number, failure/retry state, replay-safe key handling, real order list/detail navigation. |
| Backend additions | None for one-shot submit; persistent cart and order history calls should be connected if the flow is presented as a multi-session app. |
| Prohibited fiction | Payment success, shipment status, delivery ETA, coupon discount, loyalty points, or a completed order list when no API call has loaded it. |
| Recommended layout | Bag summary, confirmation step, then a separate success state; show the order number as the only success proof. |
| Recommended size | 390 x 844 and 375 x 812; reserve 34-48 px bottom space for the mobile action area. |

## Sequence Gate

Images 01-04 can be based on current runtime data after the design layout is implemented and recaptured. Images 05-06 remain design-only until their missing APIs exist. Images 07-08 can use current data, but the UniApp flow needs stable screen states and order-result rendering before they qualify as strong runtime evidence.
