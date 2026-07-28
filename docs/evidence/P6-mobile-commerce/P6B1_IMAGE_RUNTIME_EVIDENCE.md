# P6B.1 Image Runtime Evidence

## Environment

- Browser: local Chrome controlled with Playwright
- Locale: `zh-CN`
- Viewports: 390x844 and 430x932
- Device scale factor: 1
- H5: `http://127.0.0.1:5176`
- API: `http://127.0.0.1:8081`
- Image source: `src/static/assets/products`

## API-to-DOM chain

`GET /api/products` returned two Products, their `coverImagePath` values, and five SKU `imagePath` values. Product detail selection changed the hero image from black to gray using the selected SKU response. `GET /api/cart` returned the gray T-shirt and beige tote image paths. The created order `CF1785266615900` returned two OrderItems with `imagePathSnapshot`, and the order detail page displayed both snapshots.

## Browser checks

Every final page with images reported `complete=true`, `naturalWidth=1254`, `naturalHeight=1254`. All five approved assets were loaded on the product detail route. Network responses were HTTP 200 with `image/png`; no image response was an HTML fallback. Console and page errors were both zero.

## Failure behavior

When the URL is empty or an image emits `error`, `ProductImage.vue` shows `图片暂不可用` or the caller-provided historical-image placeholder. The component does not insert a letter avatar, network fallback, or invented image.

