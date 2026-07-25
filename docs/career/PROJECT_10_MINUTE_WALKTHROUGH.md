# Ten-Minute Walkthrough

1. Start MySQL and `mall-api`; open OpenAPI and show the demo catalog.
2. Explain Product versus SKU versus Inventory and the non-negative conditional update.
3. Add a SKU to the cart and submit an order with a unique `Idempotency-Key`.
4. Repeat the same request to show the original order result rather than a second deduction.
5. Change the body while reusing the key to show `IDEMPOTENCY_KEY_REUSED`.
6. Show Flyway migrations and order item snapshots.
7. Open the Vue dashboard and catalog/order views.
8. Open the UniApp user flow and ask AI product support.
9. Trace Java facts -> FastAPI structured answer -> Java Evidence/Trace.
10. State the honest limitations and ownership gap.
