# P4 Business Facts Contract

## Decision

Java is the only product-fact source. It reads Product, SKU, Inventory, and their status from CommerceFlow MySQL, validates the selection, constructs this payload, and sends it to Python. Python cannot connect to MySQL, call Java, modify stock, or invent a database fact.

P4 answers one selected SKU at a time. A product-only selection is useful for browsing, but the user must choose a real SKU before asking price, attributes, or stock questions. This avoids ambiguous availability answers.

## `businessFacts` v1

```json
{
  "question": "这件灰色基础T恤现在还有库存吗？",
  "productId": 101,
  "productCode": "PROD-1001",
  "productName": "轻盈棉质基础 T 恤",
  "productStatus": "ON_SALE",
  "productImagePath": "/assets/products/product-tshirt-gray.png",
  "skuId": 10004,
  "skuCode": "T-SHIRT-GRAY-L",
  "color": "灰色",
  "size": "L",
  "skuStatus": "ON_SALE",
  "unitPrice": "129.00",
  "currency": "CNY",
  "availableStock": 27,
  "knowledgeSnippets": [],
  "queriedAt": "2026-07-26T09:00:00Z"
}
```

| Field | Required | Type / source | P4 rule |
| --- | --- | --- | --- |
| `question` | Yes | trimmed string, Java-validated, 1-500 characters | Input to classification/answer only; do not copy into Evidence. |
| `productId` | Yes | MySQL `product.id` | Derived from selected SKU; never trusted from Python. |
| `productCode` | No | `product.product_code` | `null` if unavailable. P4 seeds contain it. |
| `productName` | Yes | `product.name` | Safe display evidence. |
| `productStatus` | Yes | `product.status` | `ON_SALE` or `OFF_SALE`; Java derives the answer boundary. |
| `productImagePath` | No | product cover or selected SKU image path | A local approved asset path is a display fact, not a provider instruction. |
| `skuId`, `skuCode`, `color`, `size` | Yes | selected `product_sku` row | The selected SKU must belong to `productId`. |
| `skuStatus` | Yes | `product_sku.status` | A selected off-sale SKU is not purchasable even when Product remains `ON_SALE`. |
| `unitPrice` | Yes | Java `BigDecimal` from `DECIMAL(19,2)`, serialized as a plain decimal string | P4B resolves the P4A `salePrice` naming conflict. Java keeps `BigDecimal`; JSON sends a string so Java/Python never use binary float for money. |
| `currency` | Yes | `product_sku.currency` | P4 expects `CNY`, but does not hard-code it. |
| `availableStock` | Yes in P4 | `inventory.available_stock` | Current schema is non-null and non-negative. `0` means out of stock; it is not missing. A future nullable schema value would mean `INSUFFICIENT_CONTEXT`, never zero. |
| `knowledgeSnippets` | Yes, empty by default | Java-authored bounded list | Reserved only for future approved, source-labelled knowledge. P4 does not implement RAG. |
| `queriedAt` | Yes | Java clock, ISO-8601 UTC | Trace/display timing only. |

## Status and Missing-data Rules

- `OFF_SALE`: answer may explain that the SKU is currently not for sale. It must not call it purchasable even when stock is positive.
- Product/SKU missing or Product/SKU mismatch: Java rejects before calling Python (`PRODUCT_NOT_FOUND`, `SKU_NOT_FOUND`, or `PRODUCT_SKU_MISMATCH`).
- `availableStock == 0`: answer may state out of stock and must not imply a restock date.
- A null/missing fact: Java returns `INSUFFICIENT_CONTEXT` or blocks the Python call. It never substitutes a guessed value.

## Prompt Allowlist and Exclusions

Only the fields above and a fixed P4 instruction may reach Python/provider input. Do not pass passwords, API keys, authorization headers, user profile data, orders, addresses, cart content, idempotency keys, raw exception details, SQL, internal provider configuration, system prompts, or trace database ids. Java uses its own safe display DTO to return facts to Vue.

## P4B Conflict Resolution

P4A used `salePrice` in its planning example and treated `productId` as optional in the external API draft. P4B adopts the smaller safe public selection contract: `productId` and `skuId` are both required, then Java verifies their real database relationship. `unitPrice` replaces `salePrice` throughout the Java/Python contract; its Java type is `BigDecimal` and its JSON form is the exact decimal string shown above. This avoids two competing facts contracts.
