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
