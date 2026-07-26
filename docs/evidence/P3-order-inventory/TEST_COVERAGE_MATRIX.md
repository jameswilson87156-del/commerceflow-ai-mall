# P3 Test Coverage Matrix

| Scenario | Test class / method | Type | Result | Remaining boundary |
| --- | --- | --- | --- | --- |
| Successful order creation | `OrderFlowTests.successfulOrderPersistsSnapshotsAmountAndMovementEvidence` | Automated Java | Passed | No concurrent multi-node load test in this Showcase scope. |
| OrderItem snapshots | `OrderFlowTests.successfulOrderPersistsSnapshotsAmountAndMovementEvidence` | Automated Java | Passed | Historical pre-V5 rows can have null separate color/size snapshots. |
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
| execution-evidence API | `OrderFlowTests.executionEvidenceApiAndMybatisReadModelExposeStoredFacts` | Automated Java / MockMvc | Passed | Read model is scoped to one order at a time. |
| MyBatis resultMap | `OrderFlowTests.executionEvidenceApiAndMybatisReadModelExposeStoredFacts` | Automated Java | Passed | Multiple future movement types are not implemented. |
| Duplicate SKU aggregation | `OrderFlowTests.duplicateSkuLinesAreAggregatedIntoOneItemAndMovement` | Automated Java | Passed | Product-level aggregation is intentionally not used; inventory remains SKU-level. |
| Database movement uniqueness | `OrderFlowTests.databasePreventsDuplicateMovementForSameOrderSkuAndType` | Automated Java | Passed | A future additional movement type requires its own explicit business rule. |
| Vue order switching | `OrderInventoryEvidence.spec.ts` `filters loaded order numbers and requests a newly selected order` | Automated Vue | Passed | Browser interaction is also verified in the real capture. |
| Vue loading | `OrderInventoryEvidence.spec.ts` `renders loading before the real order list resolves` | Automated Vue | Passed | No artificial slow-network visual test. |
| Vue empty | `OrderInventoryEvidence.spec.ts` `renders empty state when the order API returns no created orders` | Automated Vue | Passed | Empty data is test-fixture based, not a persistent demo mutation. |
| Vue error and retry | `OrderInventoryEvidence.spec.ts` `renders an error state and retries the order API` | Automated Vue | Passed | Detail-endpoint retry is component logic, not a separate isolated test. |
| Real screenshot and console check | Playwright local runtime capture | Manual reproducible browser acceptance | Passed | Screenshot is local Showcase data only. |

## Counts

- Java: 12 tests passed.
- Vue: 11 tests passed.
- The test count is not a one-to-one copy of scenario count: several tests prove multiple related invariants.
