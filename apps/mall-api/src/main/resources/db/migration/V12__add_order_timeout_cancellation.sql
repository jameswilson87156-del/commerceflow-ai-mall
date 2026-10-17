-- V12: order timeout cancellation (BACKEND_E4)
-- CREATED orders past cancel_deadline are cancelled with stock restored.
-- The deadline is computed by the application and stored explicitly so the
-- scan stays clock-consistent and portable across MySQL and H2 (MySQL mode).
-- NULL cancel_deadline means the order never auto-cancels; every order
-- created after this migration gets a deadline at insert time.

ALTER TABLE orders ADD COLUMN cancel_deadline TIMESTAMP NULL;

CREATE INDEX idx_orders_timeout_scan ON orders(status, cancel_deadline);
