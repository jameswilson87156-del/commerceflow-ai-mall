# CommerceFlow AI Evaluation V5: State Matrix and Current Offline Rerun

Date: 2026-10-22. All inputs are synthetic and all provider calls in this evidence set used local `commerceflow-mock`.

## Current offline results

| Dataset | Scope | Result | External calls |
|---|---|---:|---:|
| `customer-service-synthetic-v3` | 44 general intent and fact-contract cases | 44/44 | 0 |
| `customer-service-redteam-v10-unicode-confusables` | 27 attacks + 27 benign controls | 54/54 contract passes | 0 |
| `customer-service-composite-intents-v2-state-matrix` | 18 price + availability cases; all 8 product-status × SKU-status × stock-presence combinations, plus the one-unit boundary | 18/18 | 0 |

These are deterministic MOCK contract checks. In MOCK mode, the local classifier also supplies the recorded intent, so the 100% intent score is self-consistency of that rule path, not independent LLM accuracy. The tests check response status, expected Java-owned facts, and the declared red-team policy contract. They do not establish model quality, general jailbreak resistance, or production behavior.

Full regressions after the Java fallback repair and composite V2 data addition: Java Maven 120 tests, 0 failures, 0 errors, 10 conditional MySQL/Redis integration tests skipped; Python `py -3.12 -m pytest -q`, 178 passed.

## Reproduced Java fallback gap and repair

The AI-sidecar tests already covered price plus inventory when the item was out of stock or delisted. A Java `AiCustomerServiceTests` regression reproduced a gap in the actual mall fallback: if the provider was unavailable and the user asked both price and availability, the fallback answered only “out of stock” or “not on sale” and dropped the explicitly requested price. The Java fallback now recognizes separate price and stock clauses and includes the current fact-backed price in those two unavailable states. Stock-only questions retain their existing response. A first targeted Maven run failed at the missing price assertion; after the fix, the same test passed (1 test, 0 failures).

## Reproduction

From `services/ai-service/`:

```powershell
$env:AI_PROVIDER_MODE = "MOCK"
$env:COMMERCEFLOW_AI_EVAL_ENABLED = "false"
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-cases-v3.json --output target/evaluation/commerceflow-synthetic-v3-mock-20261022.json
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-redteam-v10-unicode-confusables.json --output target/evaluation/commerceflow-redteam-v10-mock-20261022-v2.json
py -3.12 evaluate_provider.py --dataset evaluation/customer-service-composite-intents-v2-state-matrix.json --output target/evaluation/commerceflow-composite-v2-state-matrix-mock-20261022.json
```

The composite dataset regression test also checks that V1 remains an unchanged prefix and the combined V1/V2 state set contains all eight combinations.

## Raw reports and integrity

- [General synthetic V3 result](customer-service-synthetic-v3-mock-current.json) — SHA-256 `EAC7E2802E408945FC1AA2AB39C4011EFB50DB2C7CB5E0254FBF2F69DFD275D9`
- [Red-team V10 result](customer-service-redteam-v10-mock-current.json) — SHA-256 `6ACC8A51C9932637B8997862BB15106FF0DE5502244725D0C86C4D1EDCBCFF9B`
- [Composite state-matrix V2 result](customer-service-composite-v2-state-matrix-mock.json) — SHA-256 `D0D5BC556E2BB2B6977D384AAB4B558060845AB04F54A1D876E6F13865061349`

No DeepSeek API request was sent in this rerun. The historical 22-case DeepSeek run remains a separately labeled artifact; it is not combined with these MOCK results. The exposed chat key must be rotated before a new provider run.
