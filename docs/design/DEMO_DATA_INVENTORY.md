# Demo Data Inventory

## Seed Data

Flyway V2 creates the initial account/catalog. V3 localizes the labels and V4 is the current Showcase catalog baseline: two on-sale products, five on-sale SKUs, approved local image paths, and normal/low/out-of-stock values. V9 adds database enforcement for the CNY-only order contract and positive order quantities. V10 adds an empty transactional Outbox table; it does not add Demo business data. These are the stable facts a design reference may use after a clean V1-V10 rebuild.

| Entity | ID | Value |
| --- | ---: | --- |
| User | 1 | `demo@commerceflow.local`, display name `Demo Buyer`, status `ACTIVE` |
| Category | 1 | `服饰 / 上衣` |
| Category | 2 | `配饰 / 包袋` |
| Product | 101 | `轻盈棉质基础 T 恤`; local cover image path |
| Product | 102 | `简约通勤托特包`; local cover image path |
| SKU | 10001 | `T-SHIRT-BLACK-M`, 黑色, M, CNY 129.00, stock 96 |
| SKU | 10002 | `T-SHIRT-WHITE-S`, 白色, S, CNY 129.00, stock 182 |
| SKU | 10003 | `TOTE-BEIGE-ONE`, 米色, One Size, CNY 199.00, stock 128 |
| SKU | 10004 | `T-SHIRT-GRAY-L`, 灰色, L, CNY 129.00, stock 28 |
| SKU | 10005 | `T-SHIRT-BLUE-XL`, 藏青色, XL, CNY 129.00, stock 0 |

The clean-rebuild baseline total stock is 434. Inventory is mutable: every successful order deducts stock and clears the matching cart line. A screenshot must record the capture time and should not present the seed number as a live total after orders have run.

## Historical Runtime State During the P2 Design Audit

This is a historical P2 observation, not the current P4B database state:

| SKU | Observed available stock | Reason it may differ from seed |
| --- | ---: | --- |
| `SHIRT-BLK-M` | 11 | One successful local order during showcase verification |
| `SHIRT-WHT-L` | 8 | No observed deduction |
| `TOTE-TAN-ONE` | 4 | One successful local order during showcase verification |

The API returned two `CREATED` demo orders at audit time. Their numbers and timestamps are runtime records, not fixtures that should be hard-coded into a new design image. Use the order API when capturing a current screenshot.

## Allowed Display Fields

### Product and SKU

- Product ID and name
- Description
- Category name
- SKU ID and unique SKU code
- Color and size
- Sale price as a decimal amount
- Currency
- Available stock
- Product/SKU status only when read from the current response or database rule

### Cart and Order

- User ID only as the demo context, not as a personal profile
- Cart quantity and available stock
- Order number
- Order status `CREATED`
- Total amount and currency
- Created time
- Product name, SKU code, attributes, unit price, and quantity snapshots

### AI Support

- Customer question
- Product/SKU facts passed by Java
- Structured answer suggestion
- `traceId`
- `answerStatus` (`ANSWERED`, `UNSUPPORTED_QUESTION`, `INSUFFICIENT_CONTEXT`, `PROVIDER_ERROR`, or `FALLBACK_ANSWER`)
- Provider descriptor (`commerceflow-mock` / `MOCK` by default)
- Java-owned Evidence records and compact Trace steps returned by the new ask API
- No Python-owned risk/evidence; a future trace read API remains required for history

## Forbidden or Unsupported Demo Claims

Do not add these values to a reference image from imagination:

- Today's order totals without a date-filtered API query
- Pending shipment or delivery counts
- Payment status, paid amount, refunds, or settlement
- Sales volume, conversion rate, revenue trend, or marketing performance
- AI completion rate, response-time percentile, or quality score
- Customer names beyond the demo display name
- Product images, review counts, ratings, or warehouse locations not in the model
- Trace rows that were not returned by a future read API

## Reset and Capture Discipline

Use the existing reset script before a repeatable capture when a clean database is needed. After reset, verify the product endpoint and record the current stock values. Run one documented checkout only when the design image is intended to show an order result. Never edit screenshot pixels or database values just to match the generated reference image.
