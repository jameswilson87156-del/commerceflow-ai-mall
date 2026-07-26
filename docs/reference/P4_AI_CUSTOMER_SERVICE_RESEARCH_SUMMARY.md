# P4 AI Customer Service Research Summary

## Locked Decisions

1. **Current reality:** a Java-to-Python prototype and basic `ai_trace` write exist; selected SKU, typed contracts, Java Evidence, Vue workbench, and P4 tests do not.
2. **Fact owner:** Java reads CommerceFlow MySQL. Python never reads or writes the mall database.
3. **businessFacts:** use the v1 structure in `P4_BUSINESS_FACTS_CONTRACT.md`; money is a decimal string, stock is an integer fact, and local image path is display metadata.
4. **Provider contract:** Java calls `POST /internal/ai/customer-service/answer`; Python returns only structured answer suggestion/status/provider/warning. Java validates it.
5. **Mock:** `commerceflow-mock`, mode `MOCK`, deterministic Chinese templates derived only from Java facts, no network and no hosted-model impersonation.
6. **Evidence:** generated and validated by Java from Product/SKU/Inventory facts.
7. **Trace:** extend the existing `ai_trace` with one compact summary row; do not add history UI or separate step/evidence tables.
8. **Flyway:** yes, P4 implementation needs one new forward-only V8 migration to extend `ai_trace`; P4A does not create it.
9. **UI:** three columns: real SKU selection, conversation center, fact/provider/trace/evidence right panel.
10. **Useful references:** Spring REST-client and FastAPI/Pydantic official contracts; litemall/mall domain separation; Chatwoot hierarchy; Langfuse trace vocabulary.
11. **Never copy:** all external code/assets/UI/screenshots/logos/prompts; restricted/unclear source is concept-only.
12. **Minimum file work in P4 implementation:** Java AI controller/service/client/DTOs/catalog read support/tests/config; Python models/provider/tests; Vue AI page/API/types/tests/styles/App navigation; V8 and trace docs; evidence and screenshot.
13. **Test estimate:** 52 planned verifications (22 Java, 12 Python, 17 Vue, 1 browser acceptance).
14. **Explicit exclusions:** multi-turn persistence, agents/human handoff/tickets, vector DB/RAG, product reviews, payment/logistics/refund/order privacy, Redis, queues, microservices, and production-model claims.
15. **Future P5 Redis point:** only assess a per-user plus client-IP limiter at Java's `POST /api/ai/customer-service/ask` after P4 has measurable real-provider pressure; do not introduce Redis now.
