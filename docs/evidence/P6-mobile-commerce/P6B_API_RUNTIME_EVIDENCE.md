# P6B API Runtime Evidence

## Runtime Configuration

- API base: `VITE_MOBILE_API_BASE_URL`, default `/api`.
- Development proxy target: `VITE_MOBILE_DEV_API_TARGET`, default `http://127.0.0.1:8080`.
- Asset base: `VITE_MOBILE_ASSET_BASE_URL`, default `/assets`.
- Demo user: `VITE_DEMO_USER_ID`, default `1`.
- Request timeout: 15 seconds in the shared runtime.

The checked H5 run used `VITE_MOBILE_DEV_API_TARGET=http://127.0.0.1:8081` so the browser reached the local Java API without hardcoding the target in page components.

## APIs Used

- `GET /api/products`
- `GET /api/products/{productId}`
- `GET /api/cart?userId=1`
- `POST /api/cart/items?userId=1`
- `PUT /api/cart/items/{itemId}?userId=1`
- `DELETE /api/cart/items/{itemId}?userId=1`
- `POST /api/orders?userId=1` with `Idempotency-Key`
- `GET /api/orders?userId=1`
- `GET /api/orders/{orderNo}`

The runtime handles non-2xx responses, JSON and text error bodies, timeout/abort, retryable page states and exposed rate-limit metadata. No mobile AI request is made in P6B.

## CORS

The Java whitelist is configured by `COMMERCEFLOW_CORS_ALLOWED_ORIGINS`. The final local run included the mobile H5 origin `http://127.0.0.1:5176` while retaining the existing admin origins. Wildcard origin was not added.
