# CommerceFlow Resume Bullets Plan

Use only claims traceable to frozen evidence and ownership disclosures.

- Built a local CommerceFlow AI Mall Showcase with a Java 17 Spring Boot modular monolith, MySQL/Flyway, Vue 3 admin console, UniApp H5 flow, and FastAPI provider boundary.
- Implemented a transactional order path using BigDecimal price snapshots, conditional MySQL stock decrement, `Idempotency-Key` conflict/replay handling, and persisted inventory-movement evidence.
- Designed a Java-owned grounded AI flow: selected product/SKU/inventory facts are passed to a deterministic FastAPI Mock, while Java validates responses and records Evidence and Trace; Java fact fallback remains explicit.
- Added Redis Lua fixed-window protection for the AI endpoint with HTTP 429, Retry-After, unified client cooldown, and a documented local FAIL_OPEN policy.
- Produced local verification evidence across Java, Python, Vue, and UniApp H5; cite exact current test totals only after the final release matrix reruns.

Never write that the learner independently hand-wrote the full project, that it has real users, production throughput, payment/logistics, a production model, or CI results that are still planned. Link to `docs/career/OWNERSHIP_GAPS.md` when context permits.
