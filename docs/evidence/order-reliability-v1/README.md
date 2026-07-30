# Order Reliability Evidence V1

## Verification purpose

Verify local MySQL order, inventory, idempotency, duplicate-SKU aggregation, and transaction rollback correctness. This is not a throughput, QPS, latency, or production-capacity claim.

## Environment and reproducibility

- Database: MySQL 8.4 in a dedicated temporary Docker container per run.
- Runner: `powershell -NoProfile -File .\scripts\evidence\run-order-reliability.ps1`.
- Each run creates a fresh database, waits for MySQL readiness, lets Flyway apply the existing migrations, runs only `OrderReliabilityMySqlTests`, and removes its own container in `finally`.
- Tests use isolated fixture users, products, SKUs, inventory, and request keys. Showcase orders and official seed migrations are not modified.
- Evidence logs redact local paths, local-user identifiers, and temporary connection details. No API key or `PORTFOLIO_AI_*` value is read or emitted.

## Tested scenarios and actual final state

| Scenario | Requests / setup | Success and failure classification | Before -> after / persisted facts |
| --- | --- | --- | --- |
| A Inventory competition | 50 concurrent distinct keys, stock 10, quantity 1 | 10 `CREATED`; 40 `INVENTORY_INSUFFICIENT` | stock `10 -> 0`; orders/items/movements `10/10/10`; movement transitions are continuous |
| B Concurrent idempotency | 20 concurrent same-key same-body requests, then sequential replay | one physical order; concurrent non-owner calls may be `IDEMPOTENCY_IN_PROGRESS`; replay returns original order | stock `10 -> 9`; orders/items/movements `1/1/1`; one order number |
| C Key reuse conflict | successful quantity 1, then same key quantity 2 | `IDEMPOTENCY_KEY_REUSED`, HTTP 409 equivalent | no post-conflict stock change; orders/items/movements remain `1/1/1` |
| D Transaction rollback | two SKUs: first stocked, second stock 0 | `INVENTORY_INSUFFICIENT` | first SKU restored; second unchanged; orders/items/movements `0/0/0` |
| E Duplicate-SKU aggregation | same SKU lines quantity 2 and 3 | successful aggregated order | stock `10 -> 5`; one order item; one movement; snapshot quantity 5 |

## Stability result

- Three consecutive isolated MySQL runs completed with exit code 0; per-run results are recorded in `results.json`.
- `logs/02-real-mysql-order-reliability.log` contains the MySQL test command output, with three individual run logs.
- The final normal Java regression command completed successfully; `logs/03-final-java-tests.log` records the prior sanitized successful regression evidence.

## Boundaries

- Redis is not part of order or inventory correctness.
- This evidence covers one application process and one isolated MySQL container; distributed multi-instance idempotency behavior is out of scope.
- No real AI provider, payment, shipping, refund, address, coupon, or production user data is involved.
