ALTER TABLE product ADD COLUMN product_code VARCHAR(80);
ALTER TABLE product ADD COLUMN cover_image_path VARCHAR(255);
ALTER TABLE product_sku ADD COLUMN image_path VARCHAR(255);

UPDATE product_category SET name = '服饰 / 上衣' WHERE id = 1;
UPDATE product_category SET name = '配饰 / 包袋' WHERE id = 2;

UPDATE product
SET product_code = 'PROD-1001',
    name = '轻盈棉质基础 T 恤',
    description = '日常基础款短袖，适合春夏穿搭',
    cover_image_path = '/assets/products/product-tshirt-white.png'
WHERE id = 101;

UPDATE product
SET product_code = 'PROD-1002',
    name = '简约通勤托特包',
    description = '轻便大容量，适合日常通勤使用',
    cover_image_path = '/assets/products/product-tote-beige.png'
WHERE id = 102;

UPDATE product_sku
SET sku_code = 'T-SHIRT-BLACK-M',
    color = '黑色',
    size = 'M',
    sale_price = 129.00,
    image_path = '/assets/products/product-tshirt-black.png'
WHERE id = 10001;

UPDATE product_sku
SET sku_code = 'T-SHIRT-WHITE-S',
    color = '白色',
    size = 'S',
    sale_price = 129.00,
    image_path = '/assets/products/product-tshirt-white.png'
WHERE id = 10002;

UPDATE product_sku
SET sku_code = 'TOTE-BEIGE-ONE',
    color = '米色',
    size = 'One Size',
    sale_price = 199.00,
    image_path = '/assets/products/product-tote-beige.png'
WHERE id = 10003;

INSERT INTO product_sku (id, product_id, sku_code, color, size, sale_price, currency, status, image_path) VALUES
  (10004, 101, 'T-SHIRT-GRAY-L', '灰色', 'L', 129.00, 'CNY', 'ON_SALE', '/assets/products/product-tshirt-gray.png'),
  (10005, 101, 'T-SHIRT-BLUE-XL', '藏青色', 'XL', 129.00, 'CNY', 'ON_SALE', '/assets/products/product-tshirt-navy.png');

UPDATE inventory SET available_stock = 96 WHERE sku_id = 10001;
UPDATE inventory SET available_stock = 182 WHERE sku_id = 10002;
UPDATE inventory SET available_stock = 128 WHERE sku_id = 10003;
INSERT INTO inventory (id, sku_id, available_stock) VALUES
  (4, 10004, 28),
  (5, 10005, 0);
