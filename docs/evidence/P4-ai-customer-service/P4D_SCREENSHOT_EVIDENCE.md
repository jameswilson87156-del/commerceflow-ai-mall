# P4D Screenshot Evidence

## Final screenshots

| File | Local condition | Verified PNG metadata |
| --- | --- | --- |
| `screenshots/v2/04-ai-customer-service-final-real.png` | Java, MySQL, local Python Mock, and Vue running | `1920 x 1080` |
| `screenshots/v2/04-ai-customer-service-final-fallback.png` | Java, MySQL, Vue running; local Python intentionally stopped | `1920 x 1080` |

Both captures used a local BrowserContext page with a `1920 x 1080` viewport, device scale factor `1`, `zh-CN` locale override, and `fullPage=false`. The temporary device-metric override was used only for screenshot validation and was not a project-code change.

## Visible normal evidence

- Selected SKU `10004`, `T-SHIRT-GRAY-L`, with its local product image.
- Real stock question and `commerceflow-mock / MOCK` answer.
- `fallback 否`, 7 Java-owned Evidence records, and 6 measured Trace steps.
- The final normal capture showed a real `PROVIDER_COMPLETED` duration of `4 ms`.

## Visible fallback evidence

- Python was actually stopped before the request.
- The selected SKU, product image, and stock question remain real local data.
- `java-fact-fallback / FALLBACK`, `fallback 是`, and the returned warning are visible.
- The provider Trace is marked `FALLBACK` and showed `2 ms` in the final capture.

## Earlier evidence retained

The older P4C screenshots remain unchanged:

- `04-ai-customer-service-real.png`: `1905 x 1072`.
- `04-ai-customer-service-fallback-real.png`: `1905 x 1072`.

They are retained as historical real evidence but are not presented as the P4D final-size captures. No image was cropped, stretched, padded, or used as a page background.

## Browser checks

- Final page console errors: `0`.
- Five current local product image resources returned HTTP `200` from the Vite runtime.
- No external model, external provider image, or real API key was used.
