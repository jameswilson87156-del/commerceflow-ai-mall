# P6B.1 Issues And Fixes

Only issues actually observed during this run are listed.

## Rejected blank screenshot

- Symptom: existing untracked `06-mobile-product-list-real.png` was a blank white 390x844 image.
- Cause: it did not contain a rendered page.
- Fix: moved it to `rejected-screenshots/`, added a rejection README, and committed the record before creating the hardening branch.
- Regression: new final screenshots were captured from the running page and their dimensions were checked.

## HTTP 200 HTML fallback

- Symptom: `/assets/products/*.png` returned HTTP 200 but `text/html`; no decodable image existed.
- Cause: the UniApp H5 development server did not serve the mobile `public` path for this route.
- Fix: copied the approved assets to `src/static/assets/products` and made the central resolver select the development `/src/static/assets` path and production `/static/assets` path.
- Regression: all five assets returned `image/png`, decoded to 1254x1254, and were shown.

## Shared image placeholder

- Symptom: the asset URL was present in the component root but the placeholder remained visible.
- Cause: the component template referenced bare prop names while the script held the props object.
- Fix: used explicit props bindings and a computed image source in `ProductImage.vue`.
- Regression: product list, SKU choices, cart items, confirmation items, and order snapshots rendered decoded images.

## H5 intrinsic image overflow

- Symptom: the browser document reached 1291px wide because the 1254px source image became the layout width.
- Cause: `rpx` values remained unprocessed in the Vite H5 preview, so the width constraints were ignored.
- Fix: converted the mobile showcase styles to deterministic `px` values and added global image dimensions for component-root classes.
- Regression: 390px and 430px route checks reported no horizontal overflow.

## Result action overflow

- Symptom: the result page's second action reached 396px in a 390px viewport.
- Cause: two flex buttons retained desktop-sized horizontal padding.
- Fix: reduced result action button padding and allowed the flex children to shrink.
- Regression: final result page width is exactly 390px and the final screenshot is 390x844.

