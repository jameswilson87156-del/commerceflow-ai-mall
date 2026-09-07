-- CommerceFlow Showcase exposes one money contract: all prices and orders are CNY.
-- Keep the database contract aligned with the Java API and README instead of
-- allowing an unsupported currency to enter a transaction path.
ALTER TABLE product_sku
  ADD CONSTRAINT chk_product_sku_currency_cny CHECK (currency = 'CNY');

ALTER TABLE orders
  ADD CONSTRAINT chk_orders_currency_cny CHECK (currency = 'CNY');

ALTER TABLE order_item
  ADD CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0);
