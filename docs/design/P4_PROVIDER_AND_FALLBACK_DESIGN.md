# P4 Provider and Fallback Design

## Supported P4 Questions

The deterministic Mock provider classifies only these fact-bound intents:

1. price;
2. SKU color and size;
3. current stock;
4. whether the selected SKU is currently purchasable;
5. SKU code.

It uses supplied Java facts, fixed Chinese templates, and zero randomness. It never uses external network access and never identifies itself as GPT, Claude, DeepSeek, or another hosted model.

## Unsupported and Context Cases

Shipping time, refund, payment, discount negotiation, order tracking, order privacy, delivery status, and general policy questions return `UNSUPPORTED_QUESTION`. A missing fact returns `INSUFFICIENT_CONTEXT`. No answer may invent a restock date, logistics rule, payment rule, discount, or personal-order information.

## Failure Policy

| Event | Java result | Visible meaning |
| --- | --- | --- |
| Python timeout/unavailable | Java produces a deterministic fact-only fallback if facts are sufficient. | `FALLBACK_ANSWER`, `fallbackUsed=true`, warning shown. |
| Python HTTP/provider error | Same fallback rule; otherwise `PROVIDER_ERROR`. | Never silently display an ordinary answered state. |
| Invalid JSON, unknown status, missing trace id, blank/overlong answer | Reject Python output, then apply fallback/`PROVIDER_ERROR`. | Java remains final contract validator. |
| Real-provider credentials absent | Python returns an explicit configuration condition; Java does not label it as a real answer. | Mock is used only when `AI_PROVIDER_MODE=mock`; real mode fails closed. |

P4 uses bounded timeouts and **no automatic retry**. A user-visible retry creates a new client request id and a new trace. Resilience4j is a later evaluation only after real remote-provider behavior is stable.
