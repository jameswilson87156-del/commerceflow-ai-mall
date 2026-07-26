ALTER TABLE inventory_movement
  ADD CONSTRAINT uq_inventory_movement_order_sku_type UNIQUE (order_no, sku_id, movement_type);
