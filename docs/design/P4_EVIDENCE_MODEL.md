# P4 Evidence Model

## Decision

Java generates and validates Evidence from the loaded `businessFacts`. Python supplies natural-language answer suggestion only. This prevents a provider from fabricating inventory, price, status, or SKU evidence.

```json
{
  "evidenceType": "INVENTORY",
  "field": "availableStock",
  "displayName": "当前库存",
  "value": "27",
  "sourceType": "INVENTORY",
  "sourceId": "10004"
}
```

| Field | Meaning |
| --- | --- |
| `evidenceType` | P4B's Java-owned fact category, such as `PRODUCT_FACT`, `SKU_FACT`, `PRICE_FACT`, or `INVENTORY_FACT`. |
| `field` | Contract field name, for example `skuCode` or `availableStock`. |
| `displayName` | Fixed Chinese presentation label owned by Java. |
| `value` | Safe display value formatted from Java facts; money remains a string plus currency. |
| `sourceType` | `PRODUCT`, `SKU`, or `INVENTORY`. |
| `sourceId` | Real Java database identifier. The JSON number remains a number; no provider-defined identifier is accepted. |

## Allowlist

P4 may display product name, SKU code, color, size, price, currency, current stock, and product status. A zero stock value is valid Evidence. Missing data produces no fictional Evidence card.

Never treat prompt text, model guesses, frontend constants, provider response text, unavailable payment/logistics rules, or unsourced marketing copy as Evidence. Vue renders plain text only; it does not execute provider or question content as HTML.
