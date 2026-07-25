# Architecture

CommerceFlow AI Mall V1 is a modular monolith with one separate AI side service.

```text
UniApp / Vue
      -> mall-api (Java 17, Spring Boot 3)
      -> MySQL + Flyway
      -> ai-service (Python/FastAPI, one-way request)
```

Java owns Product, SKU, Inventory, Cart, Order, money, transactions, and the authoritative `businessFacts` payload. Python receives only the facts required for the question and returns a structured answer suggestion. It cannot access MySQL, modify inventory, or call Java back.

## Order Consistency

The order transaction validates SKU state, uses `UPDATE inventory SET available_stock = available_stock - ? ... AND available_stock >= ?`, writes the order and snapshots, writes the inventory log, and clears the cart. Zero affected rows means the condition no longer holds; the transaction rolls back if any later write fails.

## Honest Boundary

No real payment, logistics, merchant settlement, production SLA, or public deployment is included. Mock Provider works without credentials. Showcase implementation and learning ownership are tracked separately.
