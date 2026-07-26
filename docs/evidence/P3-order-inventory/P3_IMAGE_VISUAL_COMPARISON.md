# P3.1 Visual Comparison

| Area | P3 design reference | Earlier real P3 screenshot | P3.1 real screenshot | Status |
| --- | --- | --- | --- | --- |
| Overall page skeleton | Left order list and right evidence detail | Implemented | Retained at 1920 x 1080 | MATCHED |
| Order-card product image | Compact thumbnail | No item image was available by design | Stored first-item thumbnail with fixed fallback state | MATCHED |
| Order-card hierarchy | Order, status, product, count, amount, time | Order, user id, amount, time | Order, status, product, count, amount, time | MATCHED |
| Item table images | Product image column | No image column | 48px snapshot image column and text fallback | MATCHED |
| Two-product order evidence | Design concept only | Single selected item in prior screenshot | Two Product/SKU snapshots and two matching movements | MATCHED |
| Inventory evidence | Before, deduct, after | Real database-backed values | Real database-backed values | MATCHED |
| Idempotency message | Request outcome, no fake rows | Initial result and explanatory copy | Initial result; replay/conflict details are collapsed developer information | PARTIALLY_MATCHED |
| Reference-only content | Visual direction | Not used as runtime content | Not used as runtime content | MATCHED |
| Payment, logistics, refunds | Not allowed | Not present | Not present | MATCHED |

## Artifacts

- Design reference: `docs/design_refs/generated/03-order-inventory-reference-v1.png`.
- Earlier real P3 screenshot: `screenshots/v2/03-order-inventory-real.png`.
- P3.1 real screenshot: `screenshots/v2/03-order-inventory-image-snapshot-real.png`.

The P3.1 screenshot is real local runtime evidence. The design reference remains an AI-generated visual reference only and is not embedded in the page.
