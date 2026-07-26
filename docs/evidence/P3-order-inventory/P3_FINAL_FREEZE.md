# P3 Final Freeze

## Frozen Baseline

- Final branch: `feat/p3-order-page-final-polish`
- Final showcase commit: `5666167dc2bffc1fbb433b330fc064785bbeaf78`
- Final screenshot: `screenshots/v2/03-order-inventory-final-real.png`
- Scope: P3 order management, order-item snapshots, inventory movement evidence, idempotency result display, and the final read-only visual polish.

## Verified Evidence

- Java: 14 tests passed.
- Vue: 13 tests passed.
- Flyway: V1 through V7 applied successfully.
- Product image requests: HTTP 200 during the final browser capture.
- Browser console errors: 0.
- Page errors: 0.
- Final capture: `1920 x 1080`, device scale factor `1`, `zh-CN`; it selects the `328.00 CNY` two-product order with two OrderItem rows and two inventory movements.

## Product Boundary

P3 supports only `CREATED / 已创建`. It does not include payment, shipment, logistics, refunds, addresses, coupons, or additional order states.

The page uses real local Java APIs, MySQL Showcase data, stored order-item image snapshots, and original local product assets. It does not embed an AI design reference or make claims about real merchants or transactions.

## Freeze Rule

P3 is frozen after this record. Do not make further visual or database changes to P3 unless a reproducible, real bug is found. Any such repair must state the bug, affected evidence, regression test or repeatable verification, and the reason the freeze exception is necessary.
