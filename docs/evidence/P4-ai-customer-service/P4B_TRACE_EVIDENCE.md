# P4B Trace and Evidence

## V8 Storage

`V8__extend_ai_trace_for_customer_service.sql` extends the existing `ai_trace` table with `client_request_id`, Product/SKU ids, `question_summary`, `answer_status`, provider/model fields, latency, fallback flag, and safe error code. It preserves old rows and does not create a separate Trace-history table.

P4B does not add an index without a real Trace search query. The existing unique `trace_id` remains the current lookup/correlation key.

## Privacy Rule

For P4B rows, both legacy required `question` and `question_summary` store a bounded category such as `商品客服：库存或购买咨询`, not the raw question. The integration database rows showed only these categories alongside request ids, statuses, provider mode, fallback flag, and null/non-null safe error codes.

## Returned Steps

The public response returns these non-persistent display steps: `REQUEST_RECEIVED`, `BUSINESS_FACTS_LOADED`, `PYTHON_REQUEST_SENT`, `PROVIDER_COMPLETED`, `RESPONSE_VALIDATED`, and `RESPONSE_RETURNED`. They contain no SQL, prompt, secret, stack trace, or raw provider payload.

## Java-owned Evidence

Java creates seven allowlisted records from current Product/SKU/Inventory data: product name/status, SKU code/color/size, price with currency, and available stock. Each record has an evidence type, display label, safe value, source type, and real source id. Python returns no Evidence field and cannot supply database evidence.
