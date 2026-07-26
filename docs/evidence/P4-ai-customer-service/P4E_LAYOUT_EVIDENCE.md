# P4E Layout Evidence

## Browser Contract

Final capture used local Chrome against the running local Vue application, Java API, MySQL, and Python service where applicable:

- viewport: `1920 x 1080`
- `deviceScaleFactor`: `1`
- locale: `zh-CN`
- zoom: `100%`
- capture mode: viewport only, no crop, no stretch, no reference image background

## Normal Mock Result

| Check | Result |
| --- | --- |
| `window.innerWidth / innerHeight` | `1920 / 1080` |
| DPR / locale / zoom | `1 / zh-CN / 100%` |
| document `clientWidth / scrollWidth` | `1920 / 1920` |
| Horizontal scrolling | absent |
| Grid bounds | `296px` to `1896px` |
| Column widths | `418.41px / 674.09px / 483.50px` |
| Five SKU cards | all within `309.83px` to `894.08px` |
| Provider, Evidence, Trace visible | yes |
| Evidence / Trace count | `7 / 6` |
| Provider | `commerceflow-mock / MOCK` |
| Fallback | `否` |
| Image requests | five local product images, all HTTP 200 |
| Console errors | `0` |

## Fallback Result

The Python service was stopped only for this controlled verification. The same viewport checks passed with document `1920 / 1920` and all core panels visible. The returned page state showed:

- Provider: `java-fact-fallback`
- Mode: `FALLBACK`
- Fallback: `是`
- Warning: Java generated the answer from current product facts because the Python service was unavailable.
- Trace: six steps; `PROVIDER_COMPLETED` and `RESPONSE_VALIDATED` are marked `FALLBACK`.
- Image requests: five local product assets, all HTTP 200.
- Console errors: `0`.

Python was restarted afterwards. A real post-restore Java request returned `commerceflow-mock / MOCK` again.
