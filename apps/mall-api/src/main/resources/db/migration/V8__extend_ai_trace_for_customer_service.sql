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

-- Existing prototype rows remain readable. P4 writes only a bounded category summary
-- to both question columns and never stores raw provider payloads or secrets.
UPDATE ai_trace
   SET question_summary = LEFT(question, 160),
       answer_status = status,
       provider_name = CASE WHEN provider_mode = 'mock' THEN 'legacy-prototype' ELSE provider_mode END,
       fallback_used = FALSE
 WHERE question_summary IS NULL;
