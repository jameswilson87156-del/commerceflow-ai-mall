# P4C Browser Evidence

## Verified Local Route

The browser used the real local chain:

```text
http://127.0.0.1:5173
  -> http://127.0.0.1:8091/api
  -> http://127.0.0.1:8000/internal/ai/customer-service/answer
```

The Java instance on 8091 was a temporary local validation instance. It used the current project source and the real local MySQL Showcase data. It was kept separate from an unrelated existing Java service on port 8081.

## Browser Acceptance Performed

- Navigated through the existing `AI 客服` entry; no new router framework was introduced.
- Loaded five real SKU cards from the product API, including images and the API-selected default `skuId=10004`.
- Used search to narrow the list by real SKU code, selected another SKU, and observed the browser-session conversation clear notice.
- Clicked a quick question and confirmed that it fills the input only; it does not fabricate an answer.
- Sent `这件灰色 L 码 T 恤现在还有库存吗？` for `skuId=10004` and received a real response: stock `28`, `commerceflow-mock`, mode `MOCK`, fallback `false`, 7 Evidence entries, and 6 Trace steps.
- Stopped only the temporary Java validation instance after catalog load. The browser preserved the original question, displayed the actual `Failed to fetch` request error, and exposed `重试请求`.
- Restored the temporary Java instance and clicked `重试请求`; the same question returned a normal `commerceflow-mock` response.
- Read browser console errors after the final normal response: **0**.
- Requested the three displayed product image URLs from the local Vite runtime; each returned HTTP **200**.

## State Coverage Notes

The visible browser run covers loading, real list, normal answer, SKU change, quick-question fill, real request error/retry, normal recovery, real fallback, and browser console inspection. Vue tests cover contract-only response states that the current local data cannot naturally produce, including `INSUFFICIENT_CONTEXT` and `PROVIDER_ERROR`.

