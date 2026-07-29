# Request Flows

## Catalog

`Vue or UniApp -> GET /api/products[/id] -> CatalogRepository/JdbcTemplate -> MySQL -> DTO -> client`.

## Cart And Order

`Client -> cart APIs -> MySQL` and `Client + Idempotency-Key -> OrderService -> transactional JDBC writes -> MySQL -> order DTO`. The order page additionally uses a focused MyBatis mapper for its read-only execution-evidence projection.

## AI Customer Service

`Client -> POST /api/ai/customer-service/ask -> Redis limiter -> Java facts query -> FastAPI -> Java validation/fallback -> Java Trace persistence -> response`.

The provider never creates an HTTP loop back to Java.
