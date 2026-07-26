# P3 Order Inventory Evidence

## Scope

P3 implements the real Chinese order-management and inventory-execution evidence page. It reads local MySQL data through the Java order APIs and does not introduce payment, shipment, logistics, refund, address, coupon, or additional order states.

## Evidence Index

- `CURRENT_BASELINE.md`: pre-P3 audit and minimum additions.
- `IMPLEMENTATION.md`: transaction, API, and UI implementation.
- `TRANSACTION_EVIDENCE.md`: atomic deduction and rollback facts.
- `IDEMPOTENCY_EVIDENCE.md`: successful replay and conflict behavior.
- `MYBATIS_READ_MODEL.md`: read-model boundary and XML result map.
- `TEST_RESULTS.md`: commands and executed results.
- `ISSUES_AND_FIXES.md`: only problems observed during P3.
- `SCREENSHOT_EVIDENCE.md`: real runtime screenshot provenance.
- `INTERVIEW_NOTES.md`: concise technical explanation.

## Boundary

Only successfully created `CREATED` orders appear in the order list. A replay and a same-key conflict are request outcomes, not fabricated extra order rows. Inventory movements come from `inventory_movement`, never from a frontend calculation based on current stock.
