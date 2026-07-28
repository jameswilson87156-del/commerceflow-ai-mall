# P6B.2 Browser Evidence

The H5 app ran at `http://127.0.0.1:5176` against the real local Java API through its Vite proxy. Chrome/Playwright used `zh-CN`, DPR 1, `visualViewport.scale` 1, and a 100% page zoom equivalent.

| Viewport | Routes | `scrollWidth / clientWidth` | Out-of-bounds core elements | Decoded product images | Console / failed request / HTTP >= 400 |
|---|---|---|---:|---|---|
| 390x844 | list, detail, cart, confirm, result, order detail | `390 / 390` for every route | 0 | all true | 0 / 0 / 0 |
| 430x932 | list, detail, cart, confirm, result, order detail | `430 / 430` for every route | 0 | all true | 0 / 0 / 0 |

At 390x844, the fixed action bar is `left=0, right=390, top=760, bottom=844, height=84`. The cart's second card ends at y=624 and the confirm page's second card ends at y=507, leaving documented non-overlapping space before the bar. Product, result, and order detail cards stay within `left=20, right=370`; at 430px they stay within `left=24, right=406`.
