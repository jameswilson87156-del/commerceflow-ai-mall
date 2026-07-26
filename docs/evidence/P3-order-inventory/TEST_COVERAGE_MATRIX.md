# P3 Test Coverage Matrix

| Scenario | Test class / method | Type | Result | Remaining boundary |
| --- | --- | --- | --- | --- |
| Successful order creation | `OrderFlowTests.successfulOrderPersistsSnapshotsAmountAndMovementEvidence` | Automated Java | Passed | No concurrent multi-node load test in this Showcase scope. |
| OrderItem snapshots and image path | `OrderFlowTests.successfulOrderPersistsRealProductIdImageSnapshotAmountAndMovementEvidence` | Automated Java | Passed | Pre-V7 orders may have a null image path. |
| Correct parent Product and SKU ids | `OrderFlowTests.dualProductOrderKeepsTwoItemsTwoMovementsAndTwoImageSnapshotsInMybatisReadModel` | Automated Java | Passed | Existing pre-P3.1 records are not rewritten. |
| BigDecimal amount | `OrderFlowTests.successfulOrderPersistsSnapshotsAmountAndMovementEvidence` | Automated Java | Passed | Mixed-currency orders are outside V1 scope. |
| Conditional atomic stock deduction | `OrderFlowTests.successfulOrderPersistsSnapshotsAmountAndMovementEvidence` | Automated Java | Passed | Database contention throughput is not benchmarked. |
| inventory_movement correctness | `OrderFlowTests.successfulOrderPersistsSnapshotsAmountAndMovementEvidence` | Automated Java | Passed | Only `ORDER_DEDUCT` exists in P3. |
| Insufficient inventory rollback | `OrderFlowTests.insufficientInventoryLeavesNoOrderInventoryOrMovement` and `failureAfterAnEarlierDeductionRollsBackEverything` | Automated Java | Passed | No externally injected database-outage test. |
| Shortage leaves no movement | `OrderFlowTests.insufficientInventoryLeavesNoOrderInventoryOrMovement` | Automated Java | Passed | Same boundary as above. |
| Same-key replay returns original order | `OrderFlowTests.sameKeySameBodyReturnsOriginalWithoutAdditionalWrites` | Automated Java | Passed | Cross-process in-flight coordination is not part of this single modular-monolith demo. |
| Replay does not deduct stock twice | `OrderFlowTests.sameKeySameBodyReturnsOriginalWithoutAdditionalWrites` | Automated Java | Passed | Same boundary as above. |
| Replay does not write movement twice | `OrderFlowTests.sameKeySameBodyReturnsOriginalWithoutAdditionalWrites` | Automated Java | Passed | Database unique key is an additional safeguard for movement rows. |
| Same key, different body returns 409 | `OrderFlowTests.reusedKeyWithDifferentBodyReturns409AndDoesNotWrite` | Automated Java / MockMvc | Passed | In-progress response is not presented as a separate historical event. |
| 409 creates no order, deduction, or movement | `OrderFlowTests.reusedKeyWithDifferentBodyReturns409AndDoesNotWrite` | Automated Java | Passed | No multi-node race test. |
| execution-evidence API | `OrderFlowTests.evidenceApiAndMybatisReadModelExposeStoredImageSnapshot` and `dualProductOrderKeepsTwoItemsTwoMovementsAndTwoImageSnapshotsInMybatisReadModel` | Automated Java / MockMvc | Passed | Read model is scoped to one order at a time. |
| MyBatis resultMap | `OrderFlowTests.dualProductOrderKeepsTwoItemsTwoMovementsAndTwoImageSnapshotsInMybatisReadModel` | Automated Java | Passed | Multiple future movement types are not implemented. |
| Legacy null image snapshot | `OrderFlowTests.legacyOrderWithNullImageSnapshotRemainsReadable` | Automated Java | Passed | It is a compatibility state, not a live-SKU fallback. |
| Duplicate SKU aggregation | `OrderFlowTests.duplicateSkuLinesAreAggregatedIntoOneItemAndMovement` | Automated Java | Passed | Product-level aggregation is intentionally not used; inventory remains SKU-level. |
| Database movement uniqueness | `OrderFlowTests.databasePreventsDuplicateMovementForSameOrderSkuAndType` | Automated Java | Passed | A future additional movement type requires its own explicit business rule. |
| Vue order switching and image rendering | `OrderInventoryEvidence.spec.ts` `renders list thumbnails, a two-item summary, and two real item image paths` and `updates the item image after selecting another order` | Automated Vue | Passed | Browser interaction is also verified in the real capture. |
| Vue loading | `OrderInventoryEvidence.spec.ts` `renders loading before the real order list resolves` | Automated Vue | Passed | No artificial slow-network visual test. |
| Vue empty | `OrderInventoryEvidence.spec.ts` `renders empty state when the order API returns no created orders` | Automated Vue | Passed | Empty data is test-fixture based, not a persistent demo mutation. |
| Vue error and retry | `OrderInventoryEvidence.spec.ts` `renders an error state and retries the order API` | Automated Vue | Passed | Detail-endpoint retry is component logic, not a separate isolated test. |
| Vue missing and failed image state | `OrderInventoryEvidence.spec.ts` `shows the missing-image state for a legacy null snapshot` and `shows the failed-image state without hiding the item text` | Automated Vue | Passed | Physical asset retention is outside this UI test. |
| Real screenshot and console check | Playwright local runtime capture | Manual reproducible browser acceptance | Passed | Screenshot is local Showcase data only. |

## Counts

- Java: 14 tests passed.
- Vue: 13 tests passed.
- The test count is not a one-to-one copy of scenario count: several tests prove multiple related invariants.
