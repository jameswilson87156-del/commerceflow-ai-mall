# P6C API Evidence

## Read path

`GET /api/products/{productId}` reloads the Product and selected SKU from local Java/MySQL before the page can ask a question. The page resolves image path, name, color, size, SKU code, price, currency, stock, and on-sale status from that response.

## Ask path

`POST /api/ai/customer-service/ask` request:

```json
{"userId":1,"productId":101,"skuId":10004,"question":"当前库存还有多少？","clientRequestId":"mobile-<uuid>"}
```

The Java response used during local verification contained Product 101, SKU 10004 (`T-SHIRT-GRAY-L`), `unitPrice` `129.00`, `currency` `CNY`, and `availableStock` `24`. It returned Java-generated Evidence (7 records) and a six-step Trace.

## Rate-limit headers

Normal local Redis responses supplied `X-RateLimit-Mode: redis`, `X-RateLimit-Limit: 5`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`. A rejected sixth request supplied HTTP 429 and `Retry-After`; the page uses that header before the body `retryAfterSeconds` fallback.
