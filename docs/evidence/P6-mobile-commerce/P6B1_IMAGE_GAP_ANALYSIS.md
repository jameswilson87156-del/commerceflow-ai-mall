# P6B.1 Image Gap Analysis

## Initial findings

The previously untracked `06-mobile-product-list-real.png` was inspected and found to be a blank white 390x844 image. It was retained under `rejected-screenshots/` with a rejection note and was not treated as evidence.

The first P6B.1 browser check exposed a more important issue: `/assets/products/*.png` returned HTTP 200 with `text/html` and a Vite fallback document. HTTP 200 alone therefore did not prove that an image loaded. The browser had no decodable `<img>` element and displayed the failure placeholder.

## Root causes and fixes

1. UniApp H5 development serves `src/static` through `/src/static/...` while the production H5 output uses `/static/...`; the central resolver now chooses the correct path by build mode.
2. The shared image component initially rendered the placeholder because its template referenced bare prop names rather than the `props` object. It now uses a computed `imageSource` and explicit `props.src`, `props.alt`, and `props.placeholder` bindings.
3. Unprocessed `rpx` values in the H5 preview allowed intrinsic 1254px images to expand the layout. Mobile showcase styles now use deterministic `px` values for the checked H5 surface.

## Final image evidence

All rows below were observed in real Playwright browser requests and DOM metrics. `complete=true` and positive natural dimensions were checked in addition to HTTP status.

| Asset | URL pattern in final run | HTTP | Content-Type | Content-Length | complete | natural size | Final shown |
| --- | --- | ---: | --- | ---: | --- | --- | --- |
| White T-shirt | `/src/static/assets/products/product-tshirt-white.png` | 200 | `image/png` | 1,154,134 | true | 1254x1254 | yes |
| Black T-shirt | `/src/static/assets/products/product-tshirt-black.png` | 200 | `image/png` | 1,525,842 | true | 1254x1254 | yes |
| Gray T-shirt | `/src/static/assets/products/product-tshirt-gray.png` | 200 | `image/png` | 1,749,667 | true | 1254x1254 | yes |
| Navy T-shirt | `/src/static/assets/products/product-tshirt-navy.png` | 200 | `image/png` | 1,554,085 | true | 1254x1254 | yes |
| Beige tote | `/src/static/assets/products/product-tote-beige.png` | 200 | `image/png` | 1,696,571 | true | 1254x1254 | yes |

The Java API returned the image paths; Vue did not hardcode business data or a SKU-to-image map.

