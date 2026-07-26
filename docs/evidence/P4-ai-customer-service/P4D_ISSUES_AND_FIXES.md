# P4D Issues and Fixes

## Fallback answered unrelated questions

- **Observation:** the prior Java fallback always generated a stock/purchase answer from facts, even when the user question was about shipping, refunds, user orders, changes, or prompt injection.
- **Root cause:** `AiService.fallback()` had no question classifier.
- **Fix:** add the focused Java fallback classifier and return `UNSUPPORTED_QUESTION` before constructing a product-fact answer for unsupported categories.
- **Regression:** Java and Vue tests plus the real stopped-Python shipping request verify the boundary.

## Trace duration values were not measurements

- **Observation:** most Trace step values were literal `0`.
- **Root cause:** the original implementation had trace structure but no per-phase clock boundaries.
- **Fix:** use `System.nanoTime()` around the six existing phases; keep Trace persistence outside reported response latency.
- **Regression:** Java verifies non-negative values and a controlled provider delay; browser evidence shows normal and fallback provider durations.

## Initial capture surface did not meet final pixel requirements

- **Observation:** the first in-app browser export produced `1672 x 1072`; its page surface was measured as `1905 x 1080` at DPR `1.5`.
- **Root cause:** the browser's ambient display metrics differed from the requested BrowserContext metrics.
- **Fix:** apply a temporary CDP device-metrics override for `1920 x 1080`, DPR `1`, and `zh-CN`, then use a viewport-only page capture. The final PNG metadata verifies `1920 x 1080`.
- **Regression:** the rejected candidate was overwritten only before it became evidence; older P4C screenshots remain intact.

## PowerShell recovery request encoded Chinese text incorrectly

- **Observation:** the first direct recovery request received an unsupported response because its Chinese question text was not transmitted as UTF-8 through the local PowerShell command path.
- **Root cause:** a plain string request body did not make the intended encoding explicit.
- **Fix:** send UTF-8 bytes with `application/json; charset=utf-8` for the direct recovery check.
- **Regression:** the repeat request returned `commerceflow-mock / MOCK`, `ANSWERED`, 7 Evidence records, and 6 Trace steps.
