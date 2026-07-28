# P6B.2 Test Coverage Matrix

Date: 2026-07-29

| Scenario | Evidence | Mode | Result |
|---|---|---|---|
| Product list and detail API | `CatalogApiTests`; browser routes | Automated + browser | Pass |
| SKU choice and zero-stock disable policy | `mobile-commerce-policy.test.mjs` | Automated | Pass |
| Normal, low, and zero stock labels | `mobile-commerce-policy.test.mjs` | Automated | Pass |
| Product image success and broken-image eligibility | `mobile-commerce-policy.test.mjs`; browser natural dimensions | Automated + browser | Pass |
| Server cart read / add | `CartApiTests`; isolated MySQL chain | Automated + runtime | Pass |
| Quantity update and delete | Cart API implementation; browser cart controls | Browser acceptance | Pass |
| Cart integer-cent total | `runtime-helpers.test.mjs` | Automated | Pass |
| Confirm reload and API error/retry UI | route state implementation; browser route checks | Browser acceptance | Pass |
| Idempotency same key | `OrderFlowTests.sameKeySameBodyReturnsOriginalWithoutAdditionalWritesOrImageSnapshots`; isolated runtime replay | Automated + runtime | Pass |
| Inventory shortage and rollback | `OrderFlowTests.insufficientInventoryLeavesNoOrderItemImageSnapshotOrMovement` and rollback test | Automated | Pass |
| 409 conflicting key | `OrderFlowTests.reusedKeyWithDifferentBodyReturns409AndDoesNotWriteOrderItem` | Automated | Pass |
| Order success and `BigDecimal` total | `OrderFlowTests`; isolated dual-item order | Automated + runtime | Pass |
| Snapshot image paths | `OrderFlowTests` image and dual-product tests; isolated database query | Automated + runtime | Pass |
| No localhost copy / no payment or logistics state | `runtime-helpers.test.mjs`; screenshot review | Automated + browser | Pass |
| Notice remains normal flow | P6B.2 browser bounds | Browser | Pass |
| Fixed action bar is visible and non-covering | P6B.2 browser bounds | Browser | Pass |
| Horizontal overflow | `mobile-commerce-policy.test.mjs`; both browser viewports | Automated + browser | Pass |
| API error and retry components | page source review and existing error/retry branches | Browser/manual | Pass |

The mobile Node suite has 18 tests. Rendered UniApp interactions are intentionally documented through reproducible Playwright browser checks rather than being misrepresented as unit-test coverage.
