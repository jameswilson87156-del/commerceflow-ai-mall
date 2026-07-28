# P6B Server Cart Evidence

## Source of Truth

The mobile cart is always loaded from `GET /api/cart?userId=1`. It does not use localStorage, a hardcoded item list, or a fake cart total.

## Verified Flow

1. Product 101, SKU `T-SHIRT-GRAY-L`, was selected and added through `POST /api/cart/items`.
2. Product 102, SKU `TOTE-BEIGE-ONE`, was selected and added through the same real API.
3. Cart response contained two real rows, image paths, currency, price and available stock.
4. Cart page rendered the two rows and calculated `129.00 + 199.00 = 328.00` using integer cents.
5. Confirm page re-read the cart before submit.
6. Successful order creation left the server cart empty.

Quantity update and deletion use user-scoped `PUT` and `DELETE` endpoints. Tests also verify another user cannot update or delete the row.
