# P4 Trace Storage Decision

## Options

| Option | Decision | Reason |
| --- | --- | --- |
| A. Return trace only | Reject | The existing project already writes `ai_trace`; losing the summary would reduce reproducibility and interview evidence. |
| B. Extend existing `ai_trace` with one privacy-minimized summary row | **Adopt for P4 implementation** | Lowest complexity while supporting a single ask, provider/fallback result, and product/SKU correlation. |
| C. New request, step, and evidence tables | Defer | Too much schema and query complexity for a single-turn Showcase; no trace-history UI is in P4. |

## P4 Implementation Migration Decision

P4 implementation requires one forward-only Flyway migration, proposed as `V8__extend_ai_trace_for_customer_service.sql`. It extends the existing table rather than creating multiple trace tables.

Planned fields: `client_request_id`, `product_id`, `sku_id`, `question_summary`, `answer_status`, `provider_name`, `provider_mode`, `model_name`, `latency_ms`, `fallback_used`, and `error_code`; reuse `trace_id`, `user_id`, `answer`, `evidence_json`, and `created_at` where appropriate. Exact nullability and indexes must be finalized against the migration baseline in the implementation task.

`question` must not receive a full sensitive prompt. P4 writes a bounded normalized summary/category to the existing non-null field and to `question_summary`; no authorization header, API key, complete system prompt, address, order content, raw provider payload, or exception stack is stored. `evidence_json` contains only Java-generated allowlisted fact evidence.

## P4B Finalization

P4B implements `V8__extend_ai_trace_for_customer_service.sql` with the planned fields. Existing `provider_mode`, `answer`, `evidence_json`, `status`, and `created_at` remain compatible for old rows; the migration backfills only safe summaries. No new index is introduced: P4B has no Trace history/search endpoint and the existing unique `trace_id` already supports its current correlation path. An index will be reconsidered only alongside a real query and `EXPLAIN` evidence.

## Trace Steps Returned to Vue

The single request response exposes a compact, ordered non-persistent display list:

1. request accepted;
2. Java loaded Product/SKU/Inventory facts;
3. Java constructed `businessFacts`;
4. Java called Python;
5. provider/Mock result received;
6. Java validated the response and generated Evidence;
7. response returned.

P4 does not add trace history or detail-read endpoints. A later explicit trace-read design is required before displaying persisted history.
