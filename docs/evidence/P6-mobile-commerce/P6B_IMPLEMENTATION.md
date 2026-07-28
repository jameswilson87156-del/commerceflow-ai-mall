# P6B Mobile Commerce Core Flow

Status: IMPLEMENTED_ON_BRANCH_REVIEW_REQUIRED

## Scope

P6B implements the real UniApp H5 mobile commerce path:

`GET /api/products` -> product detail and SKU selection -> server cart -> quantity update/delete -> cart re-read -> order submit with `Idempotency-Key` -> `CREATED` result -> order detail.

This is local Showcase functionality. It does not add authentication, payment, logistics, AI, discounts, ratings, sales counters, or localStorage as a business source of truth.

## Pages

- `pages/index/index`: product list with loading, empty, error and retry states.
- `pages/product/detail`: real product detail, SKU selection, image mapping and stock-disabled purchase.
- `pages/cart/index`: server cart, quantity update, delete and integer-cent total.
- `pages/order/confirm`: re-reads server cart and submits one idempotent order request.
- `pages/order/result`: reads the created order from the API.
- `pages/order/detail`: renders stored order and image snapshots.
- `pages/order/list`: reads the real order list.

## Backend Changes

The existing order, inventory and idempotency implementation was preserved. The minimal mobile blocker was the cart read model and write operations:

- cart responses now include `currency` and `imagePath`;
- `PUT /api/cart/items/{itemId}?userId=1` updates quantity;
- `DELETE /api/cart/items/{itemId}?userId=1` removes an item;
- item updates and deletes remain scoped by `userId`;
- CORS origins are configurable through `COMMERCEFLOW_CORS_ALLOWED_ORIGINS` and the allowed methods include GET, POST, PUT, DELETE and OPTIONS.

No Flyway migration was needed. Existing Flyway V1-V8 remains unchanged.

## Ownership Boundary

This Showcase implementation was produced with Codex assistance. It is not evidence that the learner independently wrote or can explain every module. The later learning rebuild remains the ownership-verification path.
