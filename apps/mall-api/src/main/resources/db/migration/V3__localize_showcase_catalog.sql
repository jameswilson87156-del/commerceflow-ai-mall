-- Localize only the existing showcase labels. Stable identifiers, SKU codes,
-- prices, currency, inventory quantities, and row counts are intentionally unchanged.
UPDATE product_category SET name = '日常服饰' WHERE id = 1;
UPDATE product_category SET name = '通勤配件' WHERE id = 2;

UPDATE product
SET name = '轻盈棉质基础T恤',
    description = '柔软棉质基础短袖，适合日常通勤和春夏穿搭。'
WHERE id = 101;

UPDATE product
SET name = '简约通勤托特包',
    description = '简洁轻便的通勤托特包，适合日常收纳。'
WHERE id = 102;

UPDATE product_sku SET color = '黑色' WHERE id = 10001;
UPDATE product_sku SET color = '白色' WHERE id = 10002;
UPDATE product_sku SET color = '卡其色' WHERE id = 10003;
