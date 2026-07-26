# Design Benchmark

## Inputs

- CommerceFlow P2 product/SKU design system and original product assets.
- CommerceFlow generated order reference `docs/design_refs/generated/03-order-inventory-reference-v1.png` (visual reference only, not runtime evidence).
- CommerceFlow real P3 screenshot `screenshots/v2/03-order-inventory-real.png` (runtime evidence).
- Research-only observations from the six repositories in `OPEN_SOURCE_LICENSE_MATRIX.md`.

## Locked Principles for P3.1

1. A real order page must favour the created order and its `OrderItem` snapshots before showing technical evidence.
2. Each item image is 48px and is shown only when the order evidence API returns `imagePathSnapshot`.
3. The selected order list card may show the first item image and an item count; it must not manufacture an image for an order with no snapshot.
4. `CREATED / 已创建` remains the sole business state. Payment, delivery, logistics, refund, and completion UI are prohibited.
5. Inventory movements and idempotency facts are real evidence, visually secondary, and explanatory content is collapsible.
6. The page remains a local showcase driven by real APIs and approved local assets, not a real merchant or transaction system.
