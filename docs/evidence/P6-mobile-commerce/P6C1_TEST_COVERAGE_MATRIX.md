# P6C.1 Mobile AI Final Coverage Matrix

| Scenario | Evidence | Result |
| --- | --- | --- |
| 429 keeps original composer text | real 429 browser run; `pendingQuestionForAttempt` test | PASS |
| 429 counter is not `0/500` | real 429 browser run: `8/500` | PASS |
| pending question stores trimmed text | helper unit test | PASS |
| rate card shows the blocked question | final 429 screenshot | PASS |
| send is disabled during cooldown | real 429 button inspection | PASS |
| retry is disabled during cooldown | real 429 button inspection | PASS |
| Enter/direct send uses the same `canSend` guard | `sendQuestion` -> `ask` guard source review | PASS |
| one countdown source | `cooldownRemaining` drives status, card, and buttons | PASS |
| tick never becomes negative | `nextCooldown` unit test | PASS |
| pending text survives cooldown completion | real browser retry run | PASS |
| retry uses retained text | real browser retry run | PASS |
| retry creates a fresh request ID | local `ai_trace` showed distinct `mobile-<uuid>` values | PASS |
| successful retry clears pending/draft | real browser retry run | PASS |
| SKU route initialization clears pending state | `resetForSkuChange` source and helper boundary | PASS |
| network/provider client failure retains pending text | failure branch stores `pendingQuestion` | PASS |
| normal success clears consistently | `clearQuestionAfterSuccess` unit test and normal run | PASS |
| FAIL_OPEN omits quota values | real FAIL_OPEN browser run, quota bar count `0` | PASS |
| rate state uses readable grouped layout | normal and 429 browser runs | PASS |
| no test ordinal appears in final 429 copy | final screenshot uses `现在还有库存吗？` | PASS |
| P6B/P6C regression | Java/Python/Admin/Mobile full suites | PASS |

The mobile Node suite contains 31 passing tests. Stateful HTTP, timer, fallback, and FAIL_OPEN behavior was additionally verified in local H5 browser sessions because this project does not add a component-test dependency for P6C.1.
