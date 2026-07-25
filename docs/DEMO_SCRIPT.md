# Showcase Demo Script

## 1. Start

1. Run `./scripts/start-mysql.ps1`.
2. Start `mall-api` with `MALL_API_PORT=8081` when port 8080 is occupied.
3. Start `services/ai-service` in Mock mode.
4. Open the admin preview and the mobile H5 preview.

## 2. Mobile flow

1. Show the demo buyer landing page and catalog.
2. Open Essential Cotton Shirt and choose Black / M.
3. Add the SKU to the bag.
4. Submit one order and point out the returned order number.
5. Submit the same request with the same `Idempotency-Key` and show the replayed order.
6. Ask the AI support question: `Is black M available?`
7. Point out that the answer contains current stock facts and that Java records the trace.

## 3. Admin flow

1. Show catalog readiness and stock totals.
2. Open Orders and show the transaction history.
3. Open AI Support and show the structured response.
4. Open Trace Explorer and explain `java.businessFacts` as the evidence source.

## 4. Boundary statement

This is a local showcase build with demo login, demo data, Mock Provider default mode, no real payment, no real logistics, and no production user claims. The implementation is Codex-assisted; learning ownership remains NOT_PASSED until the future learning rebuild.
