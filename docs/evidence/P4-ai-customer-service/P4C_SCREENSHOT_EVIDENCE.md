# P4C Screenshot Evidence

## Primary Normal Screenshot

- File: `screenshots/v2/04-ai-customer-service-real.png`
- Browser CSS viewport: `1920 x 1080`; device scale factor: `1`; locale: `zh-CN`.
- Selected real SKU: `10004`, gray L T-shirt, `T-SHIRT-GRAY-L`.
- User question: `这件灰色 L 码 T 恤现在还有库存吗？`
- Visible response facts: `¥129.00 CNY`, gray, L, available stock `28`.
- Visible provider state: `commerceflow-mock`, `MOCK`, `fallback 否`.
- Visible response evidence: 7 Java-owned entries grouped by PRODUCT, SKU, and INVENTORY.
- Visible trace: all 6 actual backend steps.
- Product images: first-party local Showcase assets requested with HTTP 200.
- Browser console errors: 0.

The raw Chrome capture artifact is `1905 x 1072` pixels because the browser capture surface excludes browser/scrollbar framing even though the verified CSS viewport is `1920 x 1080` at device scale factor 1. The file was not cropped, stretched, or composited.

## Fallback Screenshot

- File: `screenshots/v2/04-ai-customer-service-fallback-real.png`
- Condition: the local Python service was intentionally stopped after normal local verification.
- Source: Java's real fact-bound fallback path, not a frontend fixture.
- Expected visible provider: `java-fact-fallback`, mode `FALLBACK`, fallback true.

## Screenshot Boundaries

- Neither image embeds the AI reference image or uses it as a background.
- Neither image contains an external provider logo, a third-party customer-service image, a real API key, or a fake chat message.
- Both images are real local UI evidence. The design reference remains separate and is not runtime evidence.

