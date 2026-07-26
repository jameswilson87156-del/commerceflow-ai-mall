# P4E Layout Gap Analysis

## Scope

P4E only adjusts the Vue AI customer-service workbench, its tests, visual evidence, and P4 evidence documents. Java, Python, Flyway, MyBatis, public APIs, provider logic, fallback logic, and trace timing remain unchanged from P4D.

## Baseline Measurement

The P4D response state was inspected at the requested logical viewport (`1920 x 1080`, `deviceScaleFactor: 1`, `zh-CN`). Before the P4E changes, the three columns themselves did not exceed the document width, but the response-driven right panel expanded from its declared `782px` minimum to about `936px` high:

| Item | P4D measured state |
| --- | --- |
| Grid columns | `397.58px / 677.72px / 465.35px` |
| Grid gap | `16px` |
| Grid top / bottom | `131.48px / 1067.74px` |
| Right facts panel height | `936.26px` |
| Right Trace bottom | `1026.41px` |
| Document client width after vertical scrollbar | `1905px` |
| Document scroll width | `1905px` |

The key cause was height, not an unprotected grid minimum width: provider metadata, six fact tiles, seven Evidence entries, six Trace steps, the fixed `min-height`, heading space, and page padding together made the page taller than the requested viewport. The vertical scrollbar reduced the usable document width and made the prior visual capture unreliable for a strict `1920 x 1080` review.

The older in-app capture surface also emitted a `1672 x 1072` JPEG even when the document reported `1920 x 1080`. That image cannot prove the requested pixel-level acceptance. P4E therefore uses an isolated local Chrome capture with an explicit `1920 x 1080`, DPR 1, `zh-CN`, zoom 100 configuration for the final evidence. This is a capture-environment correction, not a claim that the old page had unmeasured horizontal DOM overflow.

## Implemented Layout Corrections

- Reduced page/header spacing and the inter-column gap while keeping the sidebar visible.
- Kept all three grid children at `min-width: 0`; the grid is explicitly `width: 100%` and remains a three-column layout above the existing responsive breakpoint.
- Compactified the five SKU cards to `54px` images, lower padding, and tighter vertical rhythm. All five cards remain visible without selector scrolling in the accepted view.
- Reduced message/composer dead space without hiding the user question, answer, Provider metadata, or send control.
- Compressed right-panel spacing. Evidence still displays seven database-derived entries and Trace still displays six real backend steps.
- Added overflow-safe technical-id presentation: long Trace IDs and Evidence details truncate only inside their local chip/field and retain the complete value in a `title` attribute.
- Retained normal wrapping for user and AI message text. Product names stay single-line/truncated in the SKU list rather than being broken character by character.

## Accepted Measurement

After P4E, native Chrome reported:

| Item | Accepted value |
| --- | --- |
| Viewport / DPR / locale / zoom | `1920 x 1080` / `1` / `zh-CN` / `100%` |
| Document client width / scroll width | `1920px / 1920px` |
| Workbench grid | `left 296px`, `right 1896px`, `width 1600px` |
| SKU column | `418.41px` |
| Chat column | `674.09px` |
| Facts/Evidence/Trace column | `483.50px` |
| Gap | `12px` |
| Five SKU card range | `309.83px` through `894.08px` |
| Right Trace bottom | `946.33px` |

All core panels, Provider, Evidence, and Trace were inside the first viewport. `scrollWidth === clientWidth`; no horizontal scroll was present.
