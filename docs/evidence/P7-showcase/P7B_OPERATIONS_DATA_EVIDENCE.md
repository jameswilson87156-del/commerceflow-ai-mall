# P7B Operations Data Evidence

The canonical local runtime used real MySQL Showcase data. At verification it contained two products, five SKUs, two low-stock SKUs, one `CREATED` double-item order, and local Showcase order amount `CNY 328.00`. The selected order had two real order items and inventory movements.

The overview reads those values from `product`, `product_sku`, `inventory`, `orders`, `order_item`, and `ai_trace`. It does not synthesize rows in Vue. The current trace counts can change when the local verifier intentionally performs one normal Mock AI request; that is expected local evidence, not a dashboard hard-coded value.

The AI summary aggregates answered, unsupported, fallback, and provider-error classifications. It exposes provider metadata only when stored trace records exist; a fresh trace table returns `NONE` / `0`.
