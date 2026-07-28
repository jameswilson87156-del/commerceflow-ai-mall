# P6B.1 Browser Evidence

## Real flow

The browser added the real gray T-shirt SKU `T-SHIRT-GRAY-L` and beige tote SKU `TOTE-BEIGE-ONE` to the server cart. The confirmation page showed two items and `¥328.00 CNY`. Submitting the existing order API created order `CF1785266615900`; the result page showed `CREATED / 已创建`, and the detail page showed both image snapshots.

## Route matrix

| Route | Viewport | API/data | Overflow | Image state | Errors |
| --- | --- | --- | ---: | --- | ---: |
| Product list | 390x844, 430x932 | `GET /api/products` | 0px | decoded | 0 |
| Product detail | 390x844, 430x932 | `GET /api/products/101` | 0px | all five assets decoded during SKU render | 0 |
| Cart | 390x844, 430x932 | `GET /api/cart`, cart update/add APIs | 0px | gray and tote decoded | 0 |
| Order confirm | 390x844, 430x932 | `GET /api/cart` | 0px | gray and tote decoded | 0 |
| Order result | 390x844, 430x932 | `GET /api/orders/{orderNo}` | 0px | not applicable | 0 |
| Order detail | 390x844, 430x932 | `GET /api/orders/{orderNo}` | 0px | snapshot images decoded | 0 |

## Boundary checks

- No horizontal scroll was present at either viewport.
- No third-party image URL was requested.
- No reference PNG was embedded as a page background.
- The order result did not claim payment, shipment, or logistics completion.
- The order detail used the snapshot image field rather than current SKU lookup.
- Browser console errors and page errors were zero.

