# P7B Browser Evidence

The Admin overview was loaded from `http://127.0.0.1:5174` through the Vite proxy to the local Java API. Chrome captured the candidate image with viewport `1920x1080`, device scale factor `1`, locale `zh-CN`, zoom `100%`, and `fullPage=false`.

The browser check recorded zero console errors, `scrollWidth == 1920`, and visual viewport scale `1`. The page shows only factual cards and panels: catalogue counts, available stock, created orders, local Showcase order amount, `ai_trace` interactions, latest created order, low-stock SKUs, AI status, and runtime boundaries.

No external network image or provider was used. The Admin image requests used the project local `/assets/products/` files and returned HTTP `200`.
