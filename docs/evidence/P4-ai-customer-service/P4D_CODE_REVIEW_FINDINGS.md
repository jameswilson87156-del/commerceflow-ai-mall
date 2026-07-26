# P4D Code Review Findings

Run date: 2026-07-26

## Findings addressed

1. `AiService.fallback()` previously generated a stock-and-price answer from facts alone. When Python was unavailable, a shipping, refund, order, price-change, inventory-change, or prompt-injection question could therefore receive an unrelated stock answer.
2. The six P4 Trace steps existed, but their `durationMs` values were mostly literal `0` values instead of measured elapsed time.
3. The earlier P4C browser captures were valid local UI evidence but their PNG metadata was `1905 x 1072`, not the requested final `1920 x 1080` size.

## Review result

- Java now classifies fallback questions before constructing a fallback answer.
- Unsupported boundaries take precedence over a purchase or stock keyword. For example, a question containing both an order-related term and a purchasability term remains unsupported.
- Trace timing now uses `System.nanoTime()` for each existing step. The existing six step identifiers and the public API shape remain unchanged.
- The response latency is captured after the response has been assembled and before `ai_trace` persistence, so database insert time is not reported as client response latency.
- No database schema, Flyway migration, public endpoint path, MyBatis SQL, or order behavior changed in P4D.

## Python and Java maintenance boundary

The deterministic Python Mock and the Java fallback intentionally retain small, separate keyword lists. Python owns normal Mock responses; Java owns the no-Python fallback. The P4D changes aligned the unsupported keywords at this product-facts boundary without copying Python provider code into Java. Future support-scope changes must update both focused rule sets and their tests.

## Current boundary

The feature remains a local Showcase product-facts assistant. It does not provide shipping, logistics, refunds, payment, discounts, user-order lookup, price or inventory mutation, system-prompt disclosure, or executable-script handling.
