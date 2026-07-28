# P6B Browser Evidence

## Final Run

- Browser: existing local Chrome controlled with Playwright.
- URL: `http://127.0.0.1:5176`.
- Viewport: 390x844.
- Device scale factor: 1.
- Locale: browser default local Chinese environment; UI content is Chinese.
- API: local Java on port 8081 through Vite proxy.

## Steps

1. Open product list and select product 101.
2. Select gray / L and confirm the SKU image and stock state.
3. Add the SKU to the server cart.
4. Open product 102 and add the beige tote SKU.
5. Open cart and confirm both rows and the `328.00 CNY` total.
6. Open confirm page, which re-reads the server cart.
7. Submit with `Idempotency-Key`.
8. Open order result and order detail for the returned order number.

## Observations

- Product, SKU, price, currency, stock, cart rows and order data came from local APIs.
- Product and snapshot image requests returned HTTP 200.
- Final flow console errors: 0.
- No network images or third-party asset requests were used.
- The final order was `CREATED`, not a payment or shipment success.
