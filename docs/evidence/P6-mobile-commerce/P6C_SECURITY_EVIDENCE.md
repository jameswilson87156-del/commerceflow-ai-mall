# P6C Security Evidence

- Answers, facts, Evidence, Trace detail, provider labels, and errors use normal text interpolation. P6C does not use `v-html`, markdown HTML rendering, script execution, or unsafe URL injection.
- The client request contains only the five API-contract fields. It does not pass stock, price, status, facts, provider values, or evidence.
- Unsupported questions such as shipping, logistics, refund, payment, coupon, other users' orders, and `script` remain outside the quick-question scope and rely on the backend safety boundary.
- Local deterministic mock mode was used. No external network provider or real API key was used.
