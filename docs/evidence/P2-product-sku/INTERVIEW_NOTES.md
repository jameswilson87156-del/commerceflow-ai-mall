# Interview Notes: Product, SKU, and Inventory

## Relationship

`Product` is the customer-facing merchandise definition: its name, description, and category describe the shared item.

`SKU` is the sellable variant under that Product. In this demo, color and size distinguish variants, and each SKU has its own `skuCode`, price, and currency.

`Inventory` is tracked per SKU rather than per Product because a black medium shirt and a white large shirt can be sold independently. The frontend receives `availableStock` with the SKU data and never invents or updates the quantity.

## What This Page Demonstrates

The list endpoint gives the admin page enough real data to show the available catalog and apply local read-only filters. Selecting a product requests its detail endpoint, then renders the returned SKU rows and inventory facts.

## Inventory Safety Boundary

The label `库存正常`, `库存偏低`, or `缺货` is only a visual interpretation. The authoritative stock value belongs to the Java service and MySQL. Order submission remains responsible for the transactional conditional stock deduction; this page has no write control and cannot change stock.

## Concise Explanation

"A Product is the common merchandise record, a SKU is the purchasable variant, and Inventory belongs to the SKU. The Vue page reads both catalog endpoints, displays `availableStock` exactly as returned, and only derives a centralized visual stock label for scanning."
