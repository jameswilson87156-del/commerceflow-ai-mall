# P4 External API Contract

## Endpoint

`POST /api/ai/customer-service/ask`

The P4 endpoint replaces the prototype-only `POST /api/ai/product-chat` for the Vue workbench. The old prototype remains outside P4 behavior until a later compatibility decision; P4 implementation must not silently make its fixed-SKU behavior appear supported.

## Request

```json
{
  "userId": 1,
  "productId": 101,
  "skuId": 10004,
  "question": "这件灰色基础T恤现在还有库存吗？",
  "clientRequestId": "p4-demo-ask-001"
}
```

| Field | Rule |
| --- | --- |
| `userId` | Required positive demo user id. It is trace attribution only; P4 never queries another user's order data. |
| `productId` | Optional positive id. If supplied, Java verifies it is the selected SKU's parent. |
| `skuId` | Required positive id. P4 answers one real SKU at a time. |
| `question` | Required after trim; 1-500 characters. Empty or longer values are rejected before any provider call. |
| `clientRequestId` | Required 1-80 character request correlation value (`[A-Za-z0-9._-]`). It is not order idempotency and P4 does not deduplicate asks. |

## Success Response

```json
{
  "traceId": "uuid",
  "clientRequestId": "p4-demo-ask-001",
  "answer": "灰色 L 码当前库存为 27 件，可以购买。",
  "answerStatus": "ANSWERED",
  "provider": {"name": "commerceflow-mock", "mode": "MOCK", "model": null},
  "evidence": [],
  "businessFacts": {},
  "trace": [],
  "latencyMs": 18,
  "fallbackUsed": false,
  "warning": null,
  "createdAt": "2026-07-26T09:00:00Z"
}
```

`evidence`, `businessFacts`, and `trace` are Java-generated or Java-validated response objects. `latencyMs` measures Java's provider call plus validation boundary, not a production SLA.

## Answer-status Decision

| Status | Meaning | UI behavior |
| --- | --- | --- |
| `ANSWERED` | Supported question answered from complete Java facts. | Normal AI answer and Evidence. |
| `INSUFFICIENT_CONTEXT` | A required fact is missing or cannot safely support the question. | Explain the limit; do not show guessed Evidence. |
| `UNSUPPORTED_QUESTION` | Payment, logistics, refund, discount, order privacy, or another out-of-scope question. | Clear boundary response; no invented policy. |
| `PROVIDER_ERROR` | Java cannot obtain a valid provider answer and no factual fallback is possible. | Error state with retry. |
| `FALLBACK_ANSWER` | Java returns a deterministic, fact-bound fallback after provider timeout/unavailable/invalid output. | Warning and retry; never claim a model answered. |

## Error Mapping

Use the existing JSON error envelope for validation and selection errors. Proposed codes: `QUESTION_INVALID`, `QUESTION_TOO_LONG`, `PRODUCT_NOT_FOUND`, `SKU_NOT_FOUND`, `SKU_PRODUCT_MISMATCH`, and `SKU_NOT_AVAILABLE`. A provider failure that produces a valid fallback is HTTP 200 with `FALLBACK_ANSWER`; only a failure with no safe answer is a 502/503-style API error according to the implementation's established exception policy.
