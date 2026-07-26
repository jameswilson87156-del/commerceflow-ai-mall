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
