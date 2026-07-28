# P5C Screenshot Evidence

All final screenshots are real local Chrome captures using Vue -> Java -> MySQL -> local FastAPI `commerceflow-mock`. They use Chinese UI, original local product images, no external network, no real API key, no design-reference background, and zero browser console errors.

| Screenshot | State | Evidence shown |
| --- | --- | --- |
| `screenshots/v2/05-ai-rate-limit-showcase-final.png` | Normal Redis response | limit 5, remaining 4, reset time, Mock answer, Provider, Business Facts, Evidence, Trace |
| `screenshots/v2/05-ai-rate-limit-429-final.png` | Real sixth request rejected | limit 5, remaining 0, countdown, retained question, disabled send/retry, no sixth successful answer |
| `screenshots/v2/05-ai-rate-limit-fail-open-final.png` | Redis stopped | degraded disclosure without quota, normal Mock answer, Provider, Business Facts, Evidence, Trace |

All five product PNG requests returned HTTP 200 from the local Vite server. The old P5B screenshots remain unchanged as historical evidence.
