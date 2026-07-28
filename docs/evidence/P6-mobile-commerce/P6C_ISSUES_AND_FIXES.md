# P6C Issues And Fixes

## Fixed: composer was clipped at 390x844

- Symptom: after the first H5 implementation, the shared `.bottom-action` `max-height: 84px` overrode the AI composer's intended `132px` height. The send button fell below the viewport.
- Root cause: the common bottom-bar rule was applied after the page class with equal specificity.
- Fix: P6C uses `.bottom-action.ai-composer` to explicitly set height, min-height, and max-height to `132px`.
- Regression evidence: browser measurement confirmed composer bounds `712..844` and send button bounds `796..836` at 390x844.

## Fixed: product context stacked instead of sharing one row

- Symptom: the product image and product copy appeared vertically stacked in H5, creating unnecessary blank space.
- Root cause: the shared `.panel` display rule overrode the page's equally specific context layout rule.
- Fix: `.ai-context.panel` explicitly uses flex layout and `.ai-context-copy` is block/flex content.
- Regression evidence: final H5 screenshot shows the image and product facts in one compact row.

No Java, FastAPI, Redis, Flyway, MySQL, order, inventory, or idempotency defect was found or changed.
