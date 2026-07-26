# P4 AI Customer Service Design Specification

**Visual reference:** `docs/design_refs/generated/04-ai-customer-service-reference-v1.png` is an AI-generated reference only. It is never a page background, runtime asset, or screenshot evidence.

## Desktop Layout

Target: `1920 x 1080`, device scale factor `1`, `zh-CN`. Retain the existing dark sidebar, light canvas, restrained 8px panels, blue primary action, cyan factual-success color, amber warning, and red error states from P2/P3.

| Region | Content | Data rule |
| --- | --- | --- |
| Left, 330px | Real Product/SKU selection list with original local thumbnail, product name, SKU code, color, size, price, stock, and sale status. | `GET /api/products` and selected product detail. No fake count or filters. |
| Center, flexible | Conversation focus: welcome state, selected-SKU context, quick questions, message list, single question input, send, loading/error/retry/unsupported/fallback messages. | Messages originate from current Vue state and P4 API response only. |
| Right, 330px | Current Java facts, provider result, compact trace steps, Evidence, latency/fallback, and collapsed development information. | Before an ask, show `未发起请求`, not a claim that a service is healthy. |

## Screenshot Acceptance Scenario

The eventual P4 real screenshot selects gray T-shirt SKU `10004`, asks `这件灰色基础T恤现在还有库存吗？`, and shows a Mock answer based on actual local stock. It visibly shows price, color, size, stock Evidence, ordered trace steps, and `Provider Mode: MOCK`. It makes no hosted-model claim and shows no payment, shipping, refund, invented customer count, or trace history.

## States and Boundaries

- Loading: send action disabled while the single current request is pending.
- Empty: no selected SKU or no prior message; no fabricated chat transcript.
- Error: provider failure with a real retry action.
- Unsupported: a neutral boundary card, not a false answer.
- Image failure: retain Product/SKU text; no external placeholder image.
- Provider/Evidence/Trace are response-driven. The development block is collapsed by default.
