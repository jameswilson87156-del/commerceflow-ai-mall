# API Contract

## Demo login

`POST /api/auth/demo-login`

```json
{"username":"demo@commerceflow.local"}
```

## Catalog

- `GET /api/products`
- `GET /api/products/{productId}`

## Cart

- `GET /api/cart?userId=1`
- `POST /api/cart/items?userId=1` with `{"skuId":10001,"quantity":1}`

## Orders

`POST /api/orders?userId=1` requires `Idempotency-Key` and accepts:

```json
{"items":[{"skuId":10001,"quantity":1}]}
```

The database key is `UNIQUE(user_id, idempotency_key)`. Same successful request returns the original order; an in-flight duplicate returns `409 IDEMPOTENCY_IN_PROGRESS`; the same key with a different body returns `409 IDEMPOTENCY_KEY_REUSED`.

- `GET /api/orders?userId=1`
- `GET /api/orders/{orderNo}`

## AI

`POST /api/ai/product-chat` calls FastAPI once with Java-owned `businessFacts`. Java stores a trace with `java.businessFacts` evidence. If the service times out or is unavailable, the API returns an `AI_FALLBACK` answer.
