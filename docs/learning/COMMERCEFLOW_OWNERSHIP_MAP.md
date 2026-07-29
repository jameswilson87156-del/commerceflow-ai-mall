# CommerceFlow Ownership Map

| Area | Showcase status | Learner ownership status | Rebuild evidence required |
| --- | --- | --- | --- |
| Product/SKU/catalog | Working local implementation | Not automatically proven | Explain DTO/query flow, rebuild one read API and UI state. |
| Cart/order/inventory | Working transactional flow | Not automatically proven | Rebuild conditional decrement/idempotency tests and explain rollback. |
| MyBatis execution evidence | Working focused read mapper | Not automatically proven | Recreate result mapping and inspect SQL/result shape. |
| AI business facts/Evidence/Trace | Working local Mock/fallback flow | Not automatically proven | Recreate contract validation and explain privacy boundary. |
| Redis limiter/429 | Working local Showcase policy | Not automatically proven | Recreate Lua decision tests and client cooldown behavior. |
| Vue admin | Working Showcase views | Not automatically proven | Rebuild one page from API/types/tests without copying. |
| UniApp H5 | Working Showcase flow | Not automatically proven | Rebuild product-to-order and AI happy/error states. |

`OWNERSHIP_VERIFICATION_STATUS` remains `NOT_PASSED` until the learner rebuild provides code, commands, test results, debug notes, change requests, and closed-book explanations. Generated Showcase code is not evidence of independent authorship.
