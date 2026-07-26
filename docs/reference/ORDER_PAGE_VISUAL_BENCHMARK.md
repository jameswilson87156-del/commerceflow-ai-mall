# Order Page Visual Benchmark

**Research-only sources:** the six repositories listed in `OPEN_SOURCE_LICENSE_MATRIX.md`, CommerceFlow's generated `03-order-inventory-reference-v1.png`, and its real P3 screenshot. No third-party screenshot, logo, image, icon, or component may enter the CommerceFlow runtime.

## What the Research Supports

| Topic | Useful observation | P3.1 application |
| --- | --- | --- |
| Order list | Compact cards or rows work when order number, status, time, amount, and a small item summary are scanable. | Retain a fixed left list; add a 48px original thumbnail per order item or a `+N` summary for additional items. |
| Order details | Separate order facts from item details; show line items as the business centre. | Keep current right-hand order facts, then place an image-aware `OrderItem` table beneath. |
| Multi-item order | One primary item plus count is less noisy than repeating every item in a list row. | Left card shows first thumbnail, first product snapshot, and `共 N 件`; right panel shows every item. |
| Status | Use a small status tag close to the order number. | Only `CREATED / 已创建` is valid in P3.1. No payment, delivery, refund, or completion tags. |
| Technical evidence | Operational proof belongs below the business detail rather than competing with it. | Keep inventory movement and idempotency in a lower evidence card; collapse explanatory text under “开发演示信息”. |
| Empty/error | Real empty and retry states prevent fake sample orders. | Preserve current loading, empty, error, and retry behaviour; do not display placeholder orders. |

## Not Suitable for CommerceFlow

- Reference-project payment, shipment, refund, coupon, marketing, address, and customer-service workflows exceed P3 scope.
- Their logos, names, screenshots, product photos, icons, static CSS, templates, and seed data cannot be copied.
- Dense enterprise filter bars are not useful until CommerceFlow exposes real filter APIs or local filters with verifiable behaviour.

## P3.1 Layout Decision

At 1920 x 1080, use an approximately 370px left order column and a flexible right detail column. A thumbnail is factual only when returned by the order evidence API from `image_path_snapshot`; it is never inferred from the current SKU image in the Vue page. The P2 blue, teal, orange, and red vocabulary remains, but red is reserved for failures and boundary explanations.
