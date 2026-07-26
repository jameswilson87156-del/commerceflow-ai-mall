# P4E Screenshot Evidence

## Final Files

| File | Real runtime state | Pixel size |
| --- | --- | --- |
| `screenshots/v2/04-ai-customer-service-showcase-final.png` | Java -> local Python -> deterministic `commerceflow-mock / MOCK` -> Java Evidence/Trace -> Vue | `1920 x 1080` |
| `screenshots/v2/04-ai-customer-service-fallback-final.png` | Java safe fallback after the local Python process is stopped | `1920 x 1080` |

Both screenshots use local Showcase data returned by the real Java API and MySQL after a Flyway V1-V8 rebuild. They are runtime evidence, not AI design references, and neither embeds a design-reference PNG.

## Visible Facts

- Selected SKU: the real gray L T-shirt SKU (`skuId 10004`).
- Normal answer: real Provider name/mode, fallback status, latency, seven Evidence entries, and six Trace steps.
- Fallback answer: `java-fact-fallback`, `FALLBACK`, fallback `是`, warning copy, and the actual fallback Trace statuses.
- All five original local product assets load from `/assets/products/` with HTTP 200.
- Browser console error count is zero for both captures.

Earlier P4C/P4D screenshots remain unchanged as historical evidence. P4E adds these two final viewport-locked files; it does not overwrite or reinterpret the older captures.
