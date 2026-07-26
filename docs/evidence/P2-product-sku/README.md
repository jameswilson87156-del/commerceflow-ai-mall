# P2 Product and SKU Management Evidence

## Scope

This evidence set covers Showcase V2 phase P2: the read-only Vue admin page for real Product, SKU, and Inventory data.

The page reads `GET /api/products` and `GET /api/products/{productId}` from the local Java API. It does not create, edit, delete, import, or bulk-change business data.

## Original Page Gap

Before this phase, the admin client only had a generic English catalog summary inside one application view. It did not provide a Chinese Product and SKU management screen with a selected-product detail area, SKU inventory table, or dedicated loading, empty, and error states.

## Evidence Index

- `IMPLEMENTATION.md`: data flow, visual decisions, and intentionally omitted scope.
- `TEST_RESULTS.md`: executed tests, build, local API checks, and browser verification.
- `ISSUES_AND_FIXES.md`: issues actually observed during this phase.
- `SCREENSHOT_EVIDENCE.md`: provenance for the real runtime screenshot.
- `INTERVIEW_NOTES.md`: concise explanation of Product, SKU, and Inventory.

## Boundary

The page is a real read-only showcase surface. Inventory labels are derived only for display and never alter the Java/MySQL inventory fact.

## P2.1 Localized Polish

P2.1 localizes the existing two-product showcase catalog for a Chinese demonstration without expanding the catalog or adding a write path. The original Flyway migrations remain unchanged; `V3__localize_showcase_catalog.sql` applies the display-text update after the already-executed schema and seed migrations.

The page remains read-only. It has no create, edit, delete, import, or bulk operation.

## P2 Design Lock

The design-lock rework keeps the same two real endpoints while rebuilding the visible structure around the approved source reference. `V4__add_showcase_product_images.sql` is a new Flyway migration: it adds API-backed product/SKU image paths, product codes, four T-shirt variants, and the local Showcase inventory states needed for normal, low, and out-of-stock presentation. The records are local interview-demo seed data, not merchant data.

`VISUAL_COMPARISON.md` records the reference comparison. `02-product-sku-design-lock-real.png` is the new real runtime screenshot; the earlier screenshots remain historical evidence.
