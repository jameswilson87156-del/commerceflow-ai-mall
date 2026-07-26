# P3.1 Order Image Page Design Specification

**Status:** DESIGN_LOCKED_FOR_IMPLEMENTATION_PENDING_USER_CONFIRMATION
**Scope:** Specification only. This document does not implement P3.1.

## Desktop Composition

Target: 1920 x 1080, device scale factor 1, `zh-CN`.

- Keep the existing deep-blue navigation and light content canvas from P2/P3.
- Place a 360-380px left order-list column beside a flexible right detail column with a 16-20px gap.
- Keep the title, user-id entry, load action, and real local API reference above the two columns. Do not add unimplemented status/search controls.
- Left cards show order number, `CREATED / 已创建`, amount, created time, first item thumbnail (48 x 48), first product snapshot, and `共 N 件` for a multi-item order.
- Right column order: basic facts; image-aware OrderItem table; inventory movement evidence; idempotency/request result; collapsed development-demo notes.

## OrderItem Table

Columns, in order: **商品图片**, 商品名称快照, SKU 编码快照, 颜色, 尺寸, 单价, 数量, 小计, 币种. Image cells are 48 x 48 with `object-fit: cover`, an accessible Chinese alt based on the product snapshot, a neutral missing-image state, and an image-load-failed state. `skuCode`, `currency`, and field identifiers remain technical values.

## Data Contract

Must come from real APIs:

- `GET /api/orders?userId={userId}`: order numbers, amounts, status, time, and item summaries.
- `GET /api/orders/{orderNo}/execution-evidence`: item snapshots, including future `imagePathSnapshot`; movement rows; idempotency key; request result; order facts.

Fixed interface copy may include: “已创建”, “库存变动与幂等结果”, “开发演示信息”, “图片暂不可用”, loading/empty/error/retry text, and the transaction-boundary explanation. No business number, amount, time, stock value, image path, order, or item is hard-coded.

## Evidence and Boundaries

- Inventory rows show SKU code, before, deducted quantity, after, movement type, and execution time from the API.
- Idempotency shows only the real key and `首次创建` or replay result returned by the API. A 409 conflict is an independent request result, never an order-list row.
- Technical explanation is collapsed by default so it does not displace OrderItem business information.
- Loading: retain existing loading state while list/evidence requests run. Empty: show no fabricated order. Error: show response error and retry. Missing/failed image: retain text item details; never substitute a reference-project image.

## Prohibited Content

No payment, unpaid/paid state, delivery, logistics, tracking, refund, receipt, coupon, marketing, customer address, supplier, or sales-performance claims. No reference screenshots/backgrounds, third-party brands, or external product images.
