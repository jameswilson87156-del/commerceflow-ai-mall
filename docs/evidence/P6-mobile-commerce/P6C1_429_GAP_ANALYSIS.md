# P6C.1 429 Gap Analysis

## Observed gap

The earlier real 429 screenshot displayed the fifth successful stock question while the sixth request was rejected. The composer had already been cleared, so it showed `0/500` and did not let the user verify or retry the rejected question.

## Root cause

`ask()` trimmed the draft, constructed a request payload, then immediately assigned `question.value = ''` before the HTTP response completed. The HTTP 429 branch retained only an error-card `question` copy; it did not maintain a single reusable pending request state. `retryRateLimited()` therefore read the error-card copy rather than a dedicated pending value.

## P6C.1 decision

- `pendingQuestion` is the only retained failed-question state.
- `ask()` stores the trimmed question in `pendingQuestion` before requesting and leaves the visible input unchanged while the request is unresolved.
- `ANSWERED` and other successful Java responses clear both the input and `pendingQuestion` consistently.
- HTTP 429, network failure, and provider failure retain the input and `pendingQuestion`.
- After cooldown, the user explicitly chooses retry. Retry reads `pendingQuestion` and constructs a fresh payload, so `createClientRequestId()` issues a new ID and no old price, stock, or businessFacts are sent.
- Product/SKU route initialization calls `resetForSkuChange()`, which clears draft, pending question, previous answer, failure state, and cooldown. A new SKU cannot inherit an old SKU's question.
- Component unmount continues to clear the cooldown timer. Its local refs are discarded with the page.

## 429 presentation

The final 429 UI keeps the normal question in both places: the composer and a `待重试问题` line in the rate-limit card. It displays only current response headers/body-derived quota values, a shared cooldown value, and disabled send/quick/retry controls. It does not add a sixth answer, Evidence, or Trace.
