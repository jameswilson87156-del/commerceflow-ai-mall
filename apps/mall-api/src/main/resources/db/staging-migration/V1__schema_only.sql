-- New empty staging/production database only. No Demo seed data.

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

ALTER TABLE product ADD COLUMN product_code VARCHAR(80);

ALTER TABLE product ADD COLUMN cover_image_path VARCHAR(255);

ALTER TABLE product_sku ADD COLUMN image_path VARCHAR(255);

ALTER TABLE order_item ADD COLUMN color_snapshot VARCHAR(60);

ALTER TABLE order_item ADD COLUMN size_snapshot VARCHAR(40);

CREATE TABLE inventory_movement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(40) NOT NULL,
  sku_id BIGINT NOT NULL,
  movement_type VARCHAR(30) NOT NULL,
  quantity INT NOT NULL CHECK (quantity > 0),
  stock_before INT NOT NULL CHECK (stock_before >= 0),
  stock_after INT NOT NULL CHECK (stock_after >= 0),
  idempotency_key VARCHAR(120) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_movement_order FOREIGN KEY (order_no) REFERENCES orders(order_no),
  CONSTRAINT fk_inventory_movement_sku FOREIGN KEY (sku_id) REFERENCES product_sku(id)
);

CREATE INDEX idx_inventory_movement_order_no ON inventory_movement(order_no);

ALTER TABLE inventory_movement
  ADD CONSTRAINT uq_inventory_movement_order_sku_type UNIQUE (order_no, sku_id, movement_type);

ALTER TABLE order_item
  ADD COLUMN image_path_snapshot VARCHAR(255) NULL;

ALTER TABLE ai_trace ADD COLUMN client_request_id VARCHAR(80) NULL;

ALTER TABLE ai_trace ADD COLUMN product_id BIGINT NULL;

ALTER TABLE ai_trace ADD COLUMN sku_id BIGINT NULL;

ALTER TABLE ai_trace ADD COLUMN question_summary VARCHAR(160) NULL;

ALTER TABLE ai_trace ADD COLUMN answer_status VARCHAR(40) NULL;

ALTER TABLE ai_trace ADD COLUMN provider_name VARCHAR(80) NULL;

ALTER TABLE ai_trace ADD COLUMN model_name VARCHAR(120) NULL;

ALTER TABLE ai_trace ADD COLUMN latency_ms BIGINT NULL;

ALTER TABLE ai_trace ADD COLUMN fallback_used BOOLEAN NULL;

ALTER TABLE ai_trace ADD COLUMN error_code VARCHAR(80) NULL;

ALTER TABLE product_sku
  ADD CONSTRAINT chk_product_sku_currency_cny CHECK (currency = 'CNY');

ALTER TABLE orders
  ADD CONSTRAINT chk_orders_currency_cny CHECK (currency = 'CNY');

ALTER TABLE order_item
  ADD CONSTRAINT chk_order_item_quantity_positive CHECK (quantity > 0);

CREATE TABLE outbox_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(120) NOT NULL UNIQUE,
  aggregate_type VARCHAR(80) NOT NULL,
  aggregate_id VARCHAR(80) NOT NULL,
  event_type VARCHAR(120) NOT NULL,
  payload_json VARCHAR(4000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_error VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_pending ON outbox_event(status, available_at, id);

CREATE INDEX idx_outbox_aggregate ON outbox_event(aggregate_type, aggregate_id);

ALTER TABLE user_account ADD COLUMN external_subject VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_user_account_external_subject ON user_account(external_subject);
