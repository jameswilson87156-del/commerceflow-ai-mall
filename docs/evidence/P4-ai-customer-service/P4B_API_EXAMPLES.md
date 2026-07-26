# P4B API Examples

## Public Ask Request

```json
{
  "userId": 1,
  "productId": 101,
  "skuId": 10004,
  "question": "灰色 L 码现在还有库存吗？",
  "clientRequestId": "p4-live-stock-001"
}
```

## Public Response Shape

The real local integration returned `ANSWERED`, provider `commerceflow-mock` / `MOCK`, `availableStock=28`, `unitPrice="129.00"`, seven Evidence entries, and six Trace steps. Dynamic trace ids and latency are intentionally omitted here.

```json
{
  "answerStatus": "ANSWERED",
  "answer": "灰色 L 码当前库存为 28 件，可以购买，售价为 ¥129.00。",
  "provider": {"name": "commerceflow-mock", "mode": "MOCK", "model": null},
  "businessFacts": {"productId": 101, "skuId": 10004, "unitPrice": "129.00", "availableStock": 28},
  "fallbackUsed": false
}
```

## Fallback Result

When the local Python service was unavailable, the same selected SKU returned `FALLBACK_ANSWER`, provider `java-fact-fallback` / `FALLBACK`, `fallbackUsed=true`, a Java fact-bound answer, and Trace `error_code=AI_SERVICE_UNAVAILABLE`.

## Selection Error

A request with Product `102` and SKU `10004` returns the existing JSON error envelope with HTTP 400 and `code=PRODUCT_SKU_MISMATCH`. No Python call or Trace row is created for an invalid selection.
