# P6B Issues And Fixes

## H5 root was blank

- Symptom: the H5 document loaded but `#app` was empty.
- Root cause: this repository's Uni alpha entry uses a custom `createApp` entry; the old `App.vue` directly mounted the single prototype page. Replacing it with an empty slot removed the page root.
- Fix: added a small H5 navigation host that maps `pages.json` routes to the real page components and uses hash navigation without Vue Router. Native Uni navigation remains the intended API boundary.

## Mobile H5 origin was rejected

- Symptom: the first browser cart write returned 403 `Invalid CORS request` from origin 5176.
- Root cause: the existing Java default whitelist covered admin ports but not the mobile H5 development port.
- Fix: made the whitelist configurable and ran the local API with `COMMERCEFLOW_CORS_ALLOWED_ORIGINS` including 5176 while preserving the existing ports.

## H5 redirect method was missing

- Symptom: the first real order was created by Java, but the H5 confirm page displayed a network error after the response.
- Root cause: the H5 navigation shim implemented `navigateTo` and `switchTab` but not `redirectTo`, so the page threw after a successful response.
- Fix: implemented `redirectTo` and verified the subsequent real order result/detail flow. The existing order remained a real `CREATED` order.

## Mobile assets returned 404

- Symptom: order snapshot image URLs were returned by the API but H5 image requests returned 404.
- Root cause: the mobile app had no own public asset directory and `/assets` was incorrectly proxied to Java.
- Fix: copied the existing five project product assets into `apps/mobile-app/public/assets/products/` and kept `/assets` as local Vite static content.

## Order result retry ref regression

- Symptom: a final route check requested `/api/orders/[object Object]` and showed an order-not-found state.
- Root cause: the retryable result-page ref was passed to `getOrder` instead of its `.value` after adding a retry key.
- Fix: the result page now stores the requested order number in one ref and passes its string value to the API. The same route was rechecked with a real existing order and the return-to-products button was also verified.

## Windows test script invocation

- Symptom: the initial mobile `node --test tests` invocation was not a valid Windows test target.
- Fix: the package script now names the portable test file explicitly as `node --test tests/runtime-helpers.test.mjs`.

## P6B.2 mobile H5 clipping and overlap

- Symptom: product detail text could leave the viewport; cart and confirm notices or item copy could overlap or stack unexpectedly; result cards could leave clipped white fragments at the left edge.
- Root cause: UniApp H5 `view` elements computed as inline boxes unless explicitly constrained. A generic panel rule then overrode page-level Flex/Grid behavior in the injected style order. Product detail also inherited a generic Hero image size.
- Fix: made page roots and generic panels block-level, made required Flex/Grid cards use higher-specificity `*.panel` selectors, scoped the product Hero image, introduced normal-flow `MobileNotice`, and standardized the 84px fixed action bar plus content padding.
- Regression: two viewports across six routes report no horizontal overflow, no core-element out-of-bounds box, and no browser errors.

## Rapid order-number collision found during full regression

- Symptom: the full Java suite intermittently treated two rapid independent orders as an idempotency-in-progress error.
- Root cause: the generated order number used only `System.currentTimeMillis()`, so a same-millisecond unique-order constraint collision was caught by the idempotency error mapping.
- Fix: generate a `CF`-prefixed UUID-based 34-character order number and add a focused rapid-order regression test. Inventory, amount, request fingerprint, idempotency-key behavior, schema, and API contracts remain unchanged.
- Regression: the complete Java suite passes 43 tests; the isolated MySQL two-item order also completed successfully.
