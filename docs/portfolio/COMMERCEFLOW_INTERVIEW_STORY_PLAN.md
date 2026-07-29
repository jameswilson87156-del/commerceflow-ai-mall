# CommerceFlow Interview Story Plan

## Three-Minute Arc

1. Explain the showcase goal: one inspectable commerce workflow with a bounded AI assistant.
2. Walk product/SKU facts into cart/order creation and show why snapshots plus conditional stock decrement matter.
3. Explain that Java, not Python, owns facts; FastAPI receives restricted facts and Java records Evidence/Trace or falls back.
4. Show Redis 429 state and name its limits honestly.
5. Close with tests, frozen evidence, Codex contribution, and the planned learner rebuild.

## Deep-Dive Prompts

- Why use `Idempotency-Key`, a request fingerprint, a MySQL uniqueness constraint, and an in-flight guard?
- What does zero affected rows from the inventory update mean, and why does rollback leave no movement?
- Why are OrderItem image/name/price fields snapshots?
- Why no Python database access or Java-Python-Java network loop?
- How do Mock, Java fallback, 429, and Redis FAIL_OPEN differ?
- What was actually verified locally, and what remains H5-only or compile-only?

## Ownership Statement

Codex materially assisted with the Showcase implementation. The learner should demonstrate understanding through the planned rebuild, closed-book explanations, focused changes, tests, and debugging. Do not equate generated code with mastery.
