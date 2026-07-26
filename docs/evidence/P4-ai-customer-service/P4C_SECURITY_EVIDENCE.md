# P4C Security Evidence

## Trust Boundary

- The browser sends only `userId`, `productId`, `skuId`, `question`, and `clientRequestId` to the public Java API.
- It does not submit price, stock, availability, product status, Evidence, Trace, authorization data, or provider configuration. Java remains the business-fact source.
- The client ID uses `crypto.randomUUID()` when available, `crypto.getRandomValues()` as a secure browser fallback, and only a non-secret local correlation fallback when neither browser API exists.
- The workbench calls Java only. Python remains an internal Java dependency, does not receive unrelated personal data, and does not write business data.

## Rendering Safety

- No `v-html` is used in the P4C workbench.
- User question and provider answer are rendered with Vue text interpolation, not HTML or Markdown-to-HTML rendering.
- The Vue test submits `<script>alert(1)</script>` as question text, confirms the text is shown literally, and confirms no script element is created.
- The UI never displays raw SQL, system prompt content, stack traces, API keys, authorization values, or full internal configuration.

## Provider Disclosure

- Normal responses visibly disclose `commerceflow-mock` and `MOCK`.
- The page states that the deterministic local Mock does not call an external hosted model.
- Fallback is visibly labelled with the real `java-fact-fallback` provider and `FALLBACK` mode.
- No real API key was configured or used in P4C validation.

