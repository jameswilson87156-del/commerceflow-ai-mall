# P6C Test Coverage Matrix

| Scenario | Evidence | Result | Remaining boundary |
| --- | --- | --- | --- |
| Real SKU required for entry | `product/detail.vue`, browser navigation | PASS | No user account selection in Showcase |
| Reload Product and selected SKU by IDs | `customer-service.vue` `loadSelection` | PASS | Product changed elsewhere requires a new navigation |
| Product name, image, price, stock context | Local browser normal run | PASS | H5 primary evidence |
| Product name quick search | `mobile-ai-customer-service.test.mjs` payload/quick UI browser | PASS | Quick questions are fixed scope by design |
| SKU-code question | `mobile-ai-customer-service.test.mjs`, recovery API run | PASS | Backend owns answer classification |
| Empty question cannot send | computed send state + API validation contract | PASS | Native keyboard behavior is platform-specific |
| Request contains only five fields | `createAskPayload` unit test | PASS | Java contract regression remains in P4 tests |
| clientRequestId UUID/fallback | unit test | PASS | Randomness is not deterministic by design |
| Normal local Mock answer | normal browser screenshot | PASS | No real model provider in P6C |
| Java Evidence source rows | normal browser response + screenshot | PASS | Mobile shows simplified rows only |
| Returned six-step Trace | unit test mapping + normal response | PASS | No SQL/prompt/stack detail exposed |
| Unsupported scope | backend P4 contract + `isSupportedQuestion` unit test | PASS | Direct arbitrary question is still backend-controlled |
| Python unavailable fallback | fallback browser screenshot | PASS | Uses existing P4 fallback |
| Provider/network failure retry UI | `requestFailure` branch code review | PASS | No forced network outage screenshot; Python outage is fallback by P4 design |
| Redis quota header display | normal browser screenshot | PASS | Values shown only when header set is complete |
| Remaining quota update | normal browser run | PASS | Redis state is intentionally local/demo-only |
| Real sixth HTTP 429 | 429 browser screenshot | PASS | Expected HTTP 429 browser resource log is documented |
| One countdown source | unit tests + 429 screenshot | PASS | Timer progression is browser-time based |
| Cooldown disables quick, retry, send | 429 browser button inspection | PASS | Keyboard submit calls the same guard |
| Cooldown zero recovery | `nextCooldown` unit test | PASS | 60-second browser wait not used as screenshot evidence |
| FAIL_OPEN has no fake quota | FAIL_OPEN browser screenshot | PASS | Existing P5 policy owns degraded decision |
| Redis recovery | post-restart local API response `mode=redis` | PASS | Browser automation session reset during same-page recovery check; API recovery was verified |
| SKU change clears local session | new page setup resets turns/facts/cooldown | PASS | No in-page SKU switcher by design |
| Script/XSS text boundary | `isSupportedQuestion` unit test; no `v-html` | PASS | Backend remains final safety boundary |
| No payment/logistics/refund flow | quick-question set and source review | PASS | Unsupported questions are not offered |
| 390x844 no horizontal overflow | normal/fallback/FAIL_OPEN browser measures | PASS | 429 page also measured no overflow |
| 430x932 no horizontal overflow | final browser viewport inspection | PASS | H5 primary, non-H5 compile-only |
| Image HTTP/render validity | normal browser natural width 1254 | PASS | Static local asset only |
| Mobile test suite | `npm test`, 28 tests | PASS | No UI component test framework added |

P6C uses the existing Node test arrangement for pure request, cooldown, quota, safety, and Trace helpers. Browser scenarios provide the stateful H5 evidence that pure tests cannot safely replace.
