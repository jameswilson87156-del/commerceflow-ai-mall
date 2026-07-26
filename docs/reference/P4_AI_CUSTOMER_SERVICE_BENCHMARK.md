# P4 AI Customer Service Benchmark

**Research date:** 2026-07-26. This is concept and interaction research only. No external source code, assets, logos, screenshots, prompts, or database schema is copied.

| Reference | What was studied | Applicable P4 lesson | Explicitly not adopted |
| --- | --- | --- |
| [litemall](https://github.com/linlinjava/litemall) | Product/SKU, cart, order, customer-service domain separation across Spring Boot and Vue. | Keep Product and SKU facts separate; select a concrete SKU before answering inventory/price questions. | Its code, table names, UI, full commerce scope, payment, address, coupon, and customer-service implementation. |
| [macrozheng/mall](https://github.com/macrozheng/mall) | Mature commerce module boundaries and separate portal/admin surfaces. | Keep P4 in the modular monolith and avoid importing broad commerce modules just to add a product Q&A path. | Its MyBatis patterns, generated models, module tree, security stack, dashboards, and code. |
| [Chatwoot](https://github.com/chatwoot/chatwoot) | Customer-support interaction hierarchy: conversation central, auxiliary context secondary. | Make the question/answer thread the visual center; keep product facts and operational information secondary. | Its omnichannel inbox, agent assignment, customer profiles, branding, UI code, and assets. |
| [Langfuse trace concepts](https://langfuse.com/docs/observability/data-model) | One request as a trace with meaningful, bounded observations. | A P4 trace should record product fact loading, Java-to-Python call, provider result, Java validation, and final response. | Langfuse UI, SDK, schema, cost/token metrics, prompt storage, or enterprise capabilities. |
| [Spring REST client reference](https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html) | Typed HTTP client and status handling options. | Replace `HttpURLConnection` and string extraction with a bounded typed client and explicit error mapping in P4 implementation. | A reactive rewrite or a new microservice boundary. |
| [FastAPI response-model documentation](https://fastapi.tiangolo.com/tutorial/response-model/) | Pydantic output schemas validate/filter response data. | Declare strict Pydantic internal request/response models and let FastAPI validate them. | Direct database access or a general agent framework. |
| [OpenAI structured-output guide](https://developers.openai.com/api/docs/guides/structured-outputs) | JSON-schema-shaped model output and detectable structured results. | Keep the optional compatible-provider boundary structured and validate it again in Java. | Any claim that Mock is OpenAI, automatic paid-model calls, or provider-specific branding in the UI. |

## Product Research Conclusion

P4 should be a **single-SKU grounded answer workspace**, not a generic chat product. A user chooses a locally real SKU, asks one product question, and receives an answer whose facts are visible and traceable. Chatwoot informs the information hierarchy only; Langfuse informs the trace vocabulary only; litemall and mall inform domain separation only.
