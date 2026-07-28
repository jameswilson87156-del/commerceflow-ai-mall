# P6B Test Coverage Matrix

| Scenario | Evidence | Type | Result |
|---|---|---|---|
| Product list from real API | `GET /api/products`, catalog page run | API + browser | Passed |
| Product detail from real API | `GET /api/products/{id}`, detail page run | API + browser | Passed |
| SKU selection and image mapping | gray / L selection; asset responses | Browser | Passed |
| Out-of-stock purchase disabled | navy XL returned `availableStock=0`; detail page disabled state | Browser | Passed |
| Server cart add | `CartApiTests`, browser add flow | Java + browser | Passed |
| Server cart update/delete | `CartApiTests`, PUT/DELETE endpoints | Java | Passed |
| Cart user scoping | `CartApiTests` wrong-user update/delete | Java | Passed |
| Cart re-read before order | confirm page and `GET /api/cart` | Browser | Passed |
| Decimal/integer-cent total | `runtime-helpers.test.mjs`, 328.00 browser total | Node + browser | Passed |
| Idempotency-Key submit | `OrderFlowTests`, confirm page header | Java + browser | Passed |
| Order transaction and stock deduction | `OrderFlowTests` | Java | Passed |
| Shortage and rollback | `OrderFlowTests` | Java | Passed |
| Replay and 409 conflict | `OrderFlowTests` | Java | Passed |
| Order result/detail | real order `CF1785264511123` | Browser | Passed |
| Loading/empty/error/retry branches | component code and real route acceptance | Manual/browser evidence | Implemented; no dedicated component-test harness yet |
| H5 error/timeout/cancel handling | shared runtime implementation | Code review | Implemented; no dedicated component-test harness yet |
| H5 build | `npm run build` | Build | Passed |
| UniApp non-H5 build | `npm run build:uni` | Build | Passed |
| Admin regression | 43 Vue tests and production build | Automated | Passed |
| Java regression | 42 tests, including 5 Redis integration tests | Automated | Passed |
| Python regression | 11 tests | Automated | Passed |

The remaining gap is dedicated rendered-page automated tests for every loading/error/empty transition. Those states are present in the page implementations and were checked as code paths, while the final happy-path pages were exercised in Playwright.
