# P4 Java-to-Python Provider Contract

## Endpoint and Direction

`POST /internal/ai/customer-service/answer`

Only Java calls Python. Python has no CommerceFlow MySQL credentials, no business write endpoint, and no Java callback. P4B removes the prototype `/v1/product-answer` path rather than retaining two core contracts. The Vue browser never calls Python directly.

## Internal Request

```json
{
  "traceId": "uuid",
  "clientRequestId": "p4-demo-ask-001",
  "question": "这件灰色基础T恤现在还有库存吗？",
  "businessFacts": {}
}
```

Python validates this with strict Pydantic models. Unknown fields are rejected or ignored by an explicit documented choice; P4 chooses **forbid unknown fields** for the internal contract.

## Internal Response

```json
{
  "traceId": "uuid",
  "answer": "灰色 L 码当前库存为 27 件，可以购买。",
  "answerStatus": "ANSWERED",
  "provider": {"name": "commerceflow-mock", "mode": "MOCK", "model": null},
  "warning": null
}
```

Python does **not** return authoritative Evidence, stock, price, product fields, trace steps, latency, or a copied `businessFacts` object. Java checks the trace id, enum, nonblank bounded answer, and provider shape; Java then attaches facts, Evidence, trace steps, fallback information, timing, and safe error codes.

## Provider Modes

| Mode | Provider name | Behavior |
| --- | --- | --- |
| `MOCK` | `commerceflow-mock` | Default. Deterministic local rule-based text from supplied facts only; no network call, no API key, no provider-brand claim. |
| `REAL_OPENAI_COMPATIBLE` | configured safe provider name | Opt-in only when base URL, key, and model are all configured. The service uses a schema-bound response then validates it. |

If real mode is selected but configuration is absent, Python returns a structured provider configuration error; Java maps it to a visible fallback or `PROVIDER_ERROR`. It must not silently report `MOCK` as a successful real-provider call.
