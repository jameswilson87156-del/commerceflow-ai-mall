# P5B Screenshot Evidence

All files are real local runtime captures at 1920x1080, device scale factor 1, and `zh-CN`. They use Vue -> Java -> local FastAPI mock with MySQL Showcase data; no AI design reference is embedded.

| File | State | Verification |
| --- | --- | --- |
| `screenshots/v2/05-ai-rate-limit-real.png` | Normal Redis success | Server-derived 5/4 card with real local mock answer, Evidence, and Trace. |
| `screenshots/v2/05-ai-rate-limit-429-real.png` | Sixth valid request rejected | Retained question, countdown, disabled submit, no invented answer. |
| `screenshots/v2/05-ai-rate-limit-fail-open-real.png` | Redis unavailable | Degraded disclosure without invented remaining quota. |

Each capture had zero browser console errors. Original local product images were confirmed by cache-cleared HTTP 200 requests.
