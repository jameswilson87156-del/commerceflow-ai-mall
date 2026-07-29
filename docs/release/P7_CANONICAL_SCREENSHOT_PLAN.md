# P7 Canonical Screenshot Plan

P7C should retain all historical `screenshots/v2` files as evidence but reference no more than four principal images in README.

| README order | Canonical file | Purpose | Preconditions |
| --- | --- | --- | --- |
| 1 | `screenshots/v2/02-product-sku-design-lock-real.png` | Real catalog/SKU and inventory facts. | API, MySQL seed, browser console check. |
| 2 | `screenshots/v2/03-order-inventory-final-real.png` | Transactional order snapshots, inventory movement, idempotency result. | P3 freeze verification. |
| 3 | `screenshots/v2/04-ai-customer-service-showcase-final.png` | Java-grounded AI answer, Evidence, Trace. | P4 freeze verification. |
| 4 | `screenshots/v2/05-ai-rate-limit-429-showcase-final.png` | Real Redis 429 state and unified cooldown. | P5 freeze verification. |

Mobile screenshots belong in a dedicated mobile section or evidence page, not in a dense README gallery. AI-generated reference images never appear as runtime evidence.

P7C must verify file dimensions, local API data, source link existence, image response success, and concise captions before promoting these four references.
