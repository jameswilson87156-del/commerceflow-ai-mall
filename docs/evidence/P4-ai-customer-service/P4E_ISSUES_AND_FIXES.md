# P4E Issues and Fixes

## 1. Response State Exceeded the First Viewport

- **Symptom:** after a real answer, the facts/evidence/trace column expanded beyond the original panel minimum and the page became taller than `1080px`.
- **Root cause:** cumulative panel content plus the original fixed heights, heading spacing, card size, and page padding. This introduced a vertical scrollbar and reduced the document client width during the old capture.
- **Fix:** compacted only the P4 Vue layout: page spacing, gap, SKU cards, composer, facts, Evidence grouping, and Trace line presentation. No backend result was removed.
- **Regression proof:** native Chrome reports `scrollWidth === clientWidth === 1920`; every core panel is visible in the accepted viewport.

## 2. Strict Vue Type Checking Rejected a New Test Assertion

- **Symptom:** the first P4E production build failed because the test called `.exists()` on `wrapper.get(...)`.
- **Root cause:** `wrapper.get(...)` already guarantees a DOM node, so the typed wrapper intentionally does not expose `.exists()`.
- **Fix:** asserted the expected layout class on the guaranteed wrapper instead.
- **Regression proof:** Vue test suite now passes 35 tests and `vue-tsc --noEmit` passes as part of `npm.cmd run build`.

## 3. Capture Surface Was Not Pixel-Equivalent to the Requested Viewport

- **Symptom:** the older in-app screenshot surface returned `1672 x 1072` although the emulated document reported `1920 x 1080`.
- **Root cause:** capture-surface dimensions did not match the requested desktop acceptance contract.
- **Fix:** final evidence uses isolated local Chrome with an explicit `1920 x 1080`, DPR 1, `zh-CN`, zoom 100 configuration.
- **Regression proof:** both P4E PNGs are exactly `1920 x 1080`; their document measurements are `clientWidth 1920` and `scrollWidth 1920`.
