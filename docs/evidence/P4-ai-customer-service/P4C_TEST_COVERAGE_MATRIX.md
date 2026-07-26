# P4C Test Coverage Matrix

| Requirement or boundary | Evidence | Type | Result | Remaining boundary |
| --- | --- | --- | --- | --- |
| AI customer-service navigation | `App.spec.ts` | Automated Vue | Pass | Existing view-switch pattern, not a URL route. |
| Catalog loading, error, retry, empty | `aiCustomerService.spec.ts` | Automated Vue | Pass | Browser run verifies live loading; API failure state is component-mocked by contract. |
| Real SKU rendering and default 10004 | `aiCustomerService.spec.ts`; browser | Automated + browser | Pass | Default falls back to first actual SKU only when 10004 is absent. |
| API image paths and image failure | `aiCustomerService.spec.ts`; local HTTP 200 checks | Automated + browser | Pass | A missing server asset intentionally renders text placeholder. |
| Product name and SKU-code search | `aiCustomerService.spec.ts`; browser SKU-code search | Automated + browser | Pass | Filtering is local over API-returned data by design. |
| Sale and inventory filters | `aiCustomerService.spec.ts` | Automated Vue | Pass | The public catalog currently returns on-sale data only. |
| Quick question | `aiCustomerService.spec.ts`; browser | Automated + browser | Pass | Fill-only behavior is intentional. |
| Empty and overlength input | `aiCustomerService.spec.ts` | Automated Vue | Pass | Maximum is the current backend-aligned 500 characters. |
| Required public request fields only | `aiCustomerService.spec.ts` | Automated Vue | Pass | Price, stock, and status facts are specifically asserted absent. |
| `clientRequestId` | `aiCustomerService.spec.ts` | Automated Vue | Pass | Browser uses secure APIs when available. |
| Rapid repeated send prevention | `aiCustomerService.spec.ts` | Automated Vue | Pass | Only one pending public request is allowed. |
| Normal answer and provider state | `aiCustomerService.spec.ts`; browser; local API matrix | Automated + browser + integration | Pass | Local default provider is deterministic Mock only. |
| `UNSUPPORTED_QUESTION` | `aiCustomerService.spec.ts`; real shipping question | Automated + integration | Pass | No shipping policy is invented. |
| `INSUFFICIENT_CONTEXT` | `aiCustomerService.spec.ts` | Contract-mocked Vue | Pass | Current Showcase data has no natural missing-fact case. |
| `PROVIDER_ERROR` | `aiCustomerService.spec.ts` | Contract-mocked Vue | Pass | Current Java production boundary maps Python outage to fact-bound fallback. |
| `FALLBACK_ANSWER` | `aiCustomerService.spec.ts`; Python-stop browser run; fallback screenshot | Automated + browser | Pass | Real fallback is intentionally not a new order or inventory action. |
| Network/request error and retry | `aiCustomerService.spec.ts`; temporary Java-stop browser run | Automated + browser | Pass | Retry reuses the original question. |
| Evidence grouping and empty state | `aiCustomerService.spec.ts`; browser | Automated + browser | Pass | Evidence comes only from Java response. |
| Six Trace steps and error tone | `aiCustomerService.spec.ts`; browser | Automated + browser | Pass | No SQL, prompt, stack trace, or key is shown. |
| SKU change clears conversation | `aiCustomerService.spec.ts`; browser | Automated + browser | Pass | Browser-session-only messages are intentionally not persisted. |
| XSS text safety and no `v-html` | `aiCustomerService.spec.ts`; source review | Automated + review | Pass | Text is not interpreted as HTML. |
| Java/Python regression and Flyway V1-V8 | Java 25; Python 11 | Automated | Pass | Java source target lock required isolated test copy. |
| Vue type check and production build | `npm.cmd run build` | Automated | Pass | None found. |

Current counts: Java **25**, Python **11**, Vue **31** tests passed.

