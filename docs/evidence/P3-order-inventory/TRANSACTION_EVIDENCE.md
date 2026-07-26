# Transaction Evidence

## Successful Deduction

The clean local demo created `CF1785048273497` through `POST /api/orders` with `Idempotency-Key: p3-demo-primary` and one unit of SKU `10001`.

Its database-backed evidence response recorded:

| Fact | Value |
| --- | --- |
| movementType | `ORDER_DEDUCT` |
| skuId | `10001` |
| stockBefore | `96` |
| quantity | `1` |
| stockAfter | `95` |
| idempotencyKey | `p3-demo-primary` |

The values come from the `inventory_movement` row, not from the current Vue inventory display.

## Failure and Rollback Coverage

`OrderFlowTests.failureAfterAnEarlierDeductionRollsBackEverything` submits an in-stock SKU followed by out-of-stock SKU `10005`. The second conditional update affects zero rows and raises an exception. The test verifies the earlier deduction is rolled back, no order is inserted, and no movement row remains for that key.

`OrderFlowTests.insufficientInventoryLeavesNoOrderInventoryOrMovement` verifies the direct shortage case with the same no-residue guarantees.

`OrderFlowTests.duplicateSkuLinesAreAggregatedIntoOneItemAndMovement` proves that two request lines for the same SKU become one quantity-three item and one quantity-three movement. V6 then rejects a manual duplicate of the same order, SKU, and `ORDER_DEDUCT` type at the database layer.
