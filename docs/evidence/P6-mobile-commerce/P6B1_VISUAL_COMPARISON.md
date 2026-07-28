# P6B.1 Visual Comparison

Status values are limited to `MATCHED`, `PARTIALLY_MATCHED`, and `NOT_MATCHED`.

| Area | Reference 07/08 intent | Final implementation | Status |
| --- | --- | --- | --- |
| Mobile page shell | Light commerce surface and compact header | Shared pale page, white panels, blue brand accents, safe-area padding | MATCHED |
| Product detail header | Back affordance, title, product context | `MobileHeader` with real product detail route | MATCHED |
| Product hero | Large product image with product identity | Real API product/SKU image, name, code, price, status | MATCHED |
| SKU selection | Visual SKU cards and selected state | Real SKU rows, image/color/size/code/stock, selected blue state | MATCHED |
| Unavailable SKU | Disabled/quiet unavailable treatment | Zero-stock SKU is visually muted and cannot be selected for add | MATCHED |
| Cart | Product image, SKU facts, quantity, total, action | Real cart rows, image fields, update/delete, BigDecimal-derived totals | MATCHED |
| Order confirmation | Review items and final amount | Real cart reread, real items and amount, user boundary note | MATCHED |
| Order result | Created result and clear status boundary | Real order number, total, item count, time, `CREATED`, no payment claim | MATCHED |
| Order detail | Historical item images and snapshot facts | `imagePathSnapshot` images and snapshot fields | MATCHED |
| Technical evidence density | Reference includes technical context | Kept only relevant user-facing facts; payment/logistics remain absent | PARTIALLY_MATCHED |
| Exact reference composition | Reference is a static visual with a fixed composition | Functional pages adapt to real content and viewport; no reference image is embedded | PARTIALLY_MATCHED |
| Real data | Reference data is illustrative | Final pages use Java API, MySQL demo data, and real order operation | MATCHED |

The remaining partial matches are intentional because the final page is a functioning mobile flow, not a static recreation or a claim of real commerce operations.

