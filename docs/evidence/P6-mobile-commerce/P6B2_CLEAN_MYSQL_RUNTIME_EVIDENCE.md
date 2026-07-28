# P6B.2 Clean MySQL Runtime Evidence

Date: 2026-07-29

## Isolated Runtime

- Compose project: `commerceflow-p6b2-runtime`
- MySQL: `mysql:8.4`, isolated host port `3317`, new named volume `commerceflow-p6b2-runtime_mysql-data`
- Redis: `redis:8.0.2`, isolated host port `6390`
- Java API: separate process on `8091`
- Both Compose health checks reached `healthy` before Java started.

## Flyway

The fresh database recorded eight successful migrations in `flyway_schema_history`:

`V1 commerceflow schema`, `V2 demo data`, `V3 localize showcase catalog`, `V4 add showcase product images`, `V5 add inventory movement evidence`, `V6 enforce unique order inventory movement`, `V7 add order item image snapshot`, and `V8 extend ai trace for customer service`.

## Real API Chain

1. `GET /api/products` returned two Showcase products and the approved local image paths.
2. `POST /api/cart/items?userId=1` added SKU `10004` (gray T-shirt) and SKU `10003` (beige tote), quantity one each.
3. `POST /api/orders?userId=1` with `Idempotency-Key: p6b2-clean-mysql-dual-item-order` created `CF1785268613246` with `328.00 CNY` and `CREATED`.
4. A repeat request with the same key and same body returned the same order number.
5. `GET /api/orders/CF1785268613246/execution-evidence` returned two OrderItem image snapshots and two inventory movements.

## Database Verification

| SKU | Snapshot path | Stock before | Deduct | Stock after |
|---|---|---:|---:|---:|
| `T-SHIRT-GRAY-L` | `/assets/products/product-tshirt-gray.png` | 28 | 1 | 27 |
| `TOTE-BEIGE-ONE` | `/assets/products/product-tote-beige.png` | 128 | 1 | 127 |

The MySQL `orders` row has total `328.00`, currency `CNY`, and status `CREATED`. This is a real empty-volume MySQL run, not an H2 substitute.

## Cleanup

After evidence collection, the Java process on port `8091` and the exact `commerceflow-p6b2-runtime` Compose project, network, containers, and named volume were removed. Existing development services on ports `3307`, `6380`, `8081`, and `5176` were not part of this cleanup.
