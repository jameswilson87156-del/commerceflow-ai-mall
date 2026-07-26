# P4B Provider Evidence

## Default Provider

The default provider is `commerceflow-mock` with `providerMode=MOCK`. It is deterministic, has no random branch, performs no network call, and uses only the Java-supplied typed facts.

It supports price, color/size, current stock, purchasability, and SKU code. It returns Chinese `UNSUPPORTED_QUESTION` for shipping, refunds, payments, discounts, order privacy, prompt injection, attempts to change facts, and script-like input.

## Real-provider Boundary

`REAL_OPENAI_COMPATIBLE` is a fail-closed placeholder. It returns a configuration failure and has no implementation that calls a remote service. P4B used neither external network nor a real API key.

## Fallback

Timeout, unavailable service, invalid response, or provider-error status creates a Java fact-only fallback. The returned provider is `java-fact-fallback` / `FALLBACK`, `answerStatus=FALLBACK_ANSWER`, `fallbackUsed=true`, and a visible warning explains that Java generated the response from current facts.
