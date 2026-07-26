# P4D Fallback Boundary Evidence

## Real stopped-Python checks

The P4D validation Python process on local port `8000` was deliberately stopped after normal local verification. Java remained available on local port `8091` and continued to query the local MySQL Showcase catalog.

### Supported stock question

- Question: `这件灰色 L 码 T 恤现在还有库存吗？`
- Selected SKU: `10004`, `T-SHIRT-GRAY-L`
- Provider: `java-fact-fallback / FALLBACK`
- `fallbackUsed`: `true`
- `answerStatus`: `FALLBACK_ANSWER`
- Answer used the current local fact: stock `28`, sale price `¥129.00`.
- The real `PROVIDER_COMPLETED` Trace step was `FALLBACK` and recorded `2 ms` in the final fallback browser run.

### Unsupported shipping question

A local UTF-8 API request asked `什么时候发货？` while Python remained stopped.

- Provider: `java-fact-fallback / FALLBACK`
- `fallbackUsed`: `true`
- `answerStatus`: `UNSUPPORTED_QUESTION`
- Trace steps: `6`
- The answer did **not** contain the selected SKU demo price `129.00` or its stock quantity `28`.
- The warning states both conditions: the AI service is unavailable and the question is outside the local product-facts support scope.

## Priority rule

Unsupported topics are checked before product-fact categories. This prevents an order, shipping, refund, policy-change, or injection request from being reinterpreted as a purchasability question merely because it also contains a stock or purchase keyword.

## Data and safety boundary

Java fallback does not call Python again, access external network services, write Product, SKU, Inventory, Cart, or Order data, or invent commercial policy. It only reads the already-loaded product facts used by the same request.
