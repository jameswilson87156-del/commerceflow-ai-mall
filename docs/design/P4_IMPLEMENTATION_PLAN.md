# P4 Implementation Plan

## Preconditions

P3 remains frozen. P4 begins only with a separate implementation instruction and branch from this research commit. No P4A code artifact is implementation-ready by itself.

## Ordered Work

1. Add typed Java DTOs and validation for `POST /api/ai/customer-service/ask`; add catalog read/query support that can load on-sale and off-sale facts without hard-coded SKU selection.
2. Replace Java hand-built JSON/string parsing with a bounded typed HTTP client and internal P4 request/response objects.
3. Update FastAPI models, deterministic Mock classifier/templates, real-compatible fail-closed behavior, and tests. Keep Python database-free.
4. Add V8 only after final SQL review: extend `ai_trace` according to `P4_TRACE_STORAGE_DECISION.md` and add migration tests.
5. Add Java Java-owned Evidence, compact trace steps, fallback mapping, and tests.
6. Build Vue AI workbench using real Product APIs and the new Java endpoint; no direct browser-to-Python call.
7. Execute Java/Python/Vue/browser matrix, produce only a real runtime screenshot, then update P4 evidence.

## Definition of Done

Mock mode runs without a key; selection, facts, answer, provider, Evidence, trace, fallback, and UI all originate from real local services. The final screenshot is a real P4 run, not the design reference. No unsupported domain is implied.
