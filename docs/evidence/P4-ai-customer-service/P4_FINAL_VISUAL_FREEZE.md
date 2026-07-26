# P4 Final Visual Freeze

## Frozen Branch Baseline

P4 is visually frozen at the P4E acceptance commit on `feat/p4-ai-customer-service-final-layout`.

## Accepted Runtime Evidence

- Normal final screenshot: `screenshots/v2/04-ai-customer-service-showcase-final.png`
- Fallback final screenshot: `screenshots/v2/04-ai-customer-service-fallback-final.png`
- Both files: `1920 x 1080`, DPR 1, `zh-CN`, zoom 100, local runtime, console errors `0`, product image HTTP status `200`.
- MySQL was recreated from an empty local Compose volume and Flyway V1-V8 applied successfully.
- Java: 28 passed. Python: 11 passed. Vue: 35 passed. Vue production build: passed.

## Supported Boundary

Only Product/SKU facts, current inventory facts, a single customer-service answer, Java Evidence, and six-step Trace are showcased. The page supports `CREATED` order evidence elsewhere in the project but P4 does not add payment, logistics, shipment, refunds, addresses, coupons, Trace history, real API keys, or external network calls.

## Freeze Rule

No further P4 visual, database, or behavior change is planned. A change is allowed only to correct a reproducible real defect, with a new focused test and updated runtime evidence.
