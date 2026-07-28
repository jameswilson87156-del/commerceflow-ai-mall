# P6C Browser Evidence

The H5 page was exercised at 390x844 DPR 1, zh-CN, zoom 100%. Normal, fallback, and FAIL_OPEN browser sessions had zero JavaScript `pageerror` events and zero horizontal overflow. Product image requests resolved locally and had natural width `1254`.

The real HTTP 429 scenario produces Chrome's expected failed-resource console line for the deliberately rejected request; there were no application JavaScript exceptions. The screenshot shows the handled page state rather than an unhandled error.

The fixed composer initially conflicted with the shared `84px` bottom action cap. P6C overrides that cap for this page to `132px`; the final send button is fully visible at 390x844.
