# Reimplementation Log

**Status:** P3.1 implemented independently. No source code, SQL, Vue component, template, image, or screenshot has been copied from a reference repository.

| Reference idea | CommerceFlow independent approach | Copied code | Actual difference | Test evidence |
| --- | --- | --- | --- | --- |
| Product/SKU and order-item snapshot separation | Preserve current `product`, `product_sku`, `orders`, and `order_item` terminology; implement one image-path snapshot through V7. | No | CommerceFlow has a narrower P3 status model and local-only assets. | 14 Java tests include snapshot, legacy-null, and multi-product assertions. |
| Order list plus detail information hierarchy | Retain CommerceFlow's left-list/right-detail layout and place evidence below business data. | No | No payment, shipment, refund, marketing, or customer data. | Vue tests and `03-order-inventory-image-snapshot-real.png`. |
| Compact order thumbnails | Render the stored snapshot path returned by local APIs, never a reference asset or current-SKU lookup. | No | Uses only approved CommerceFlow AI-generated product assets. | Vue missing-image and load-failure tests. |
| Inventory audit visibility | Retain `inventory_movement` and its order/SKU/type uniqueness constraint. | No | Reference projects do not determine CommerceFlow's transaction model. | P3 transaction and P3.1 regression tests. |

Any future desire to reuse even a short licensed snippet must stop implementation and obtain user confirmation with the exact source path, original license, proposed range, copyright notice requirements, and an independent alternative.
