INSERT INTO user_account (id, username, display_name, status) VALUES (1, 'demo@commerceflow.local', 'Demo Buyer', 'ACTIVE');
INSERT INTO product_category (id, name, status, sort_order) VALUES (1, 'Everyday Wear', 'ACTIVE', 1), (2, 'Work Essentials', 'ACTIVE', 2);
INSERT INTO product (id, category_id, name, description, status) VALUES
  (101, 1, 'Essential Cotton Shirt', 'Soft cotton shirt with a clean everyday silhouette.', 'ON_SALE'),
  (102, 2, 'Structured Work Tote', 'Laptop-friendly tote with a simple internal organizer.', 'ON_SALE');
INSERT INTO product_sku (id, product_id, sku_code, color, size, sale_price, currency, status) VALUES
  (10001, 101, 'SHIRT-BLK-M', 'Black', 'M', 129.00, 'CNY', 'ON_SALE'),
  (10002, 101, 'SHIRT-WHT-L', 'White', 'L', 129.00, 'CNY', 'ON_SALE'),
  (10003, 102, 'TOTE-TAN-ONE', 'Tan', 'One Size', 299.00, 'CNY', 'ON_SALE');
INSERT INTO inventory (id, sku_id, available_stock) VALUES (1, 10001, 12), (2, 10002, 8), (3, 10003, 5);
