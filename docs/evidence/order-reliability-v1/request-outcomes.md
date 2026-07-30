# Request outcomes

- Distinct idempotency keys: exactly the available stock can create orders; exhausted requests return `INVENTORY_INSUFFICIENT`.
- Same key and same body: one physical order; concurrent callers may receive `IDEMPOTENCY_IN_PROGRESS`; a later replay returns the original order.
- Same key and different body: `IDEMPOTENCY_KEY_REUSED` (HTTP 409 at the API boundary) with no extra write.
- Outcome counts and final persistence counts are recorded in `results.json`.
