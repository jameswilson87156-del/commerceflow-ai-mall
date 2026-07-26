# Idempotency Evidence

## Real Local Demo Requests

The local PowerShell script `scripts/seed-order-evidence.ps1` called the running Java API after a clean MySQL rebuild.

| Request | Result | Database consequence |
| --- | --- | --- |
| `p3-demo-primary`, SKU `10001`, quantity `1` | created `CF1785048273497` | one order, item, and movement |
| same key and same body | returned `CF1785048273497` | no second order, item, stock deduction, or movement |
| same key and quantity `2` | HTTP `409`, `IDEMPOTENCY_KEY_REUSED` | no new order, deduction, or movement |
| `p3-demo-shortage`, SKU `10005`, quantity `1` | HTTP `400`, `INVENTORY_INSUFFICIENT` | no order or movement |

The script asserted that exactly three successful API requests created rows. The final real list contained three orders: `CF1785048273497`, `CF1785048273764`, and `CF1785048273842`.

## Implementation Rule

The MySQL constraint `UNIQUE (user_id, idempotency_key)` and request fingerprint preserve one successful order result per key/user. The service returns that existing order only if the fingerprint is equal; otherwise the global error handler maps the conflict to HTTP 409.
