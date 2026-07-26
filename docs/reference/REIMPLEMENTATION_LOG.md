# Reimplementation Log

**Status:** Research stage. No source code, SQL, Vue component, template, image, or screenshot has been copied from a reference repository.

| Reference idea | CommerceFlow independent approach | Copied code | Actual difference | Test evidence |
| --- | --- | --- | --- | --- |
| Product/SKU and order-item snapshot separation | Preserve current `product`, `product_sku`, `orders`, and `order_item` terminology; recommend a single image-path snapshot. | No | CommerceFlow has a narrower P3 status model and local-only assets. | No implementation in this branch |
| Order list plus detail information hierarchy | P3.1 spec retains CommerceFlow's left-list/right-detail layout and moves evidence below business data. | No | No payment, shipment, refund, marketing, or customer data. | No implementation in this branch |
| Compact order thumbnails | Require snapshot path from the evidence API, not a reference asset or current-SKU lookup. | No | Uses only approved CommerceFlow AI-generated product assets. | No implementation in this branch |
| Inventory audit visibility | Retain existing `inventory_movement` and its order/SKU/type uniqueness constraint. | No | Reference projects do not determine CommerceFlow's transaction model. | Existing P3 tests; no change here |

Any future desire to reuse even a short licensed snippet must stop implementation and obtain user confirmation with the exact source path, original license, proposed range, copyright notice requirements, and an independent alternative.
