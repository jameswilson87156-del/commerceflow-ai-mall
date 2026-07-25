# Demo Data Inventory

## Seed Data

Flyway V2 creates one demo account, two on-sale products, three on-sale SKUs, and inventory baseline values. These are the stable facts a design reference may use.

| Entity | ID | Value |
| --- | ---: | --- |
| User | 1 | `demo@commerceflow.local`, display name `Demo Buyer`, status `ACTIVE` |
| Category | 1 | `Everyday Wear` |
| Category | 2 | `Work Essentials` |
| Product | 101 | `Essential Cotton Shirt`; Soft cotton shirt with a clean everyday silhouette. |
| Product | 102 | `Structured Work Tote`; Laptop-friendly tote with a simple internal organizer. |
| SKU | 10001 | `SHIRT-BLK-M`, Black, M, CNY 129.00, baseline stock 12 |
| SKU | 10002 | `SHIRT-WHT-L`, White, L, CNY 129.00, baseline stock 8 |
| SKU | 10003 | `TOTE-TAN-ONE`, Tan, One Size, CNY 299.00, baseline stock 5 |

The baseline total stock is 25. Inventory is mutable: every successful order deducts stock and clears the matching cart line. A screenshot must record the capture time and should not present the seed number as a live total after orders have run.

## Observed Runtime State During This Audit

At the time of this design audit, the running local API returned:

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
- `providerMode` (`mock` by default)
- `status` (`COMPLETED` or `AI_FALLBACK`)
- Evidence tag `java.businessFacts`
- Python risk/evidence only after Java exposes it through a trace read contract

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
