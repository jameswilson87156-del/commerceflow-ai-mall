# MyBatis Read Model

## Narrow Role

P3 adds `mybatis-spring-boot-starter` only for `OrderEvidenceMapper`. Existing order creation, inventory deduction, item insert, cart cleanup, and transaction control remain in `OrderService` and `OrderRepository` using `JdbcTemplate`.

## Mapper Shape

- `OrderEvidenceMapper.java`: read-only mapper interface.
- `OrderEvidenceMapper.xml`: XML `resultMap` for an order, nested OrderItem snapshots, and nested inventory movements.
- `OrderExecutionEvidenceDto`: page-ready DTO used by `GET /api/orders/{orderNo}/execution-evidence`.
- `OrderEvidenceMybatisConfig`: explicit DataSource, XML resource, and mapper scan configuration.

The query joins `orders`, `order_item`, `inventory_movement`, and `product_sku`. It does not write business data and does not duplicate JDBC write logic.

## Result-Map Proof

`OrderFlowTests.executionEvidenceApiAndMybatisReadModelExposeStoredFacts` reads the mapper directly and through HTTP. It verifies one nested item and one movement with stock `28 -> 26` for a real test order.
