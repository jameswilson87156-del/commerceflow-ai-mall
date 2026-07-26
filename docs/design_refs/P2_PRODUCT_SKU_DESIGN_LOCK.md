# P2 Product SKU Design Lock

## Goal

Implement the Product and SKU page as a real-data version of `source/product-sku/target-page.png`. The target controls the page structure, hierarchy, density, and visual language; it is not embedded in the application and is not runtime evidence.

## Desktop Layout

The primary acceptance viewport is `1920 x 1080` at `deviceScaleFactor: 1` and `zh-CN`.

- Fixed deep navy sidebar: 272px wide, with CommerceFlow AI Mall branding, four navigation entries, a system-boundary block, and footer metadata.
- Main area: pale gray workspace with a title/search row followed by a 426px master list and a flexible detail column.
- Master list: two visual product cards, each with a square original product image, real summary fields, and real aggregate values derived from the returned SKU list.
- Detail column: product identity card, SKU table, then inventory-fact card.
- State colors: blue selection, teal normal stock, orange low stock, red out of stock.

## Data Contract

The page remains read-only and uses `GET /api/products` plus `GET /api/products/{productId}`. Product code, status, cover image path, and SKU image path are returned by the Java API. All counts, price values, statuses, stock values, and image paths come from MySQL Showcase data.

## Explicit Exclusions

No product write actions, supplier data, sales metrics, restock history, import/export, payment, logistics, or reference-image background are part of this page.
