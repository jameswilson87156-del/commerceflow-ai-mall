CREATE TABLE user_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(80) NOT NULL UNIQUE,
  display_name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  status VARCHAR(20) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  name VARCHAR(180) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id)
);

CREATE TABLE product_sku (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  sku_code VARCHAR(80) NOT NULL UNIQUE,
  color VARCHAR(60) NOT NULL,
  size VARCHAR(40) NOT NULL,
  sale_price DECIMAL(19,2) NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'CNY',
  status VARCHAR(20) NOT NULL,
  CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku_id BIGINT NOT NULL UNIQUE,
  available_stock INT NOT NULL CHECK (available_stock >= 0),
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
);

CREATE TABLE cart_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  quantity INT NOT NULL CHECK (quantity > 0),
  selected BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, sku_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_cart_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
);

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(40) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  idempotency_key VARCHAR(120) NOT NULL,
  request_fingerprint VARCHAR(128) NOT NULL,
  total_amount DECIMAL(19,2) NOT NULL,
  currency VARCHAR(10) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, idempotency_key),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  product_name_snapshot VARCHAR(180) NOT NULL,
  sku_code_snapshot VARCHAR(80) NOT NULL,
  sku_attributes_snapshot VARCHAR(180) NOT NULL,
  unit_price DECIMAL(19,2) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE inventory_change_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku_id BIGINT NOT NULL,
  change_type VARCHAR(30) NOT NULL,
  quantity INT NOT NULL,
  order_no VARCHAR(40) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_trace (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trace_id VARCHAR(60) NOT NULL UNIQUE,
  user_id BIGINT,
  question VARCHAR(500) NOT NULL,
  provider_mode VARCHAR(30) NOT NULL,
  answer VARCHAR(2000) NOT NULL,
  evidence_json VARCHAR(4000) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
