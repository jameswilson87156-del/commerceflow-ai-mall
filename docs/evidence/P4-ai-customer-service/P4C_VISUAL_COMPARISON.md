# P4C Visual Comparison

Reference: `docs/design_refs/generated/04-ai-customer-service-reference-v1.png`.

The reference is a visual-layout guide only. The table below compares it with the real P4C workbench; it does not assign a fabricated similarity percentage.

| Area | Result | Real P4C implementation and reason |
| --- | --- | --- |
| Three-column workbench | MATCHED | Left selection, center question/answer, right facts/Evidence/Trace are present at the desktop target viewport. |
| Deep navigation and light work area | MATCHED | Reuses CommerceFlow's established navigation and work-surface system. |
| Real SKU selection | MATCHED | Five API-returned SKU cards; no frontend product fixture. |
| Product imagery | MATCHED | First-party local Showcase image paths supplied by the real product API. |
| Product/SKU filters | MATCHED | Search and both filters are functional local operations over API-loaded data. |
| Conversational focal area | MATCHED | Single-turn request, answer cards, input states, quick questions, and retry are real interactions. |
| Provider disclosure | MATCHED | Displays actual `commerceflow-mock`, `MOCK`, latency, and fallback state rather than a hosted-model claim. |
| Business Facts | MATCHED | Uses selection preview before a request, then explicitly uses the Java response facts for the answered turn. |
| Evidence | MATCHED | Seven Java-owned items group under PRODUCT, SKU, and INVENTORY. |
| Trace | MATCHED | All six server-returned steps render in a restrained vertical timeline. |
| Loading, unsupported, error, fallback | MATCHED | Implemented as response-driven or real request states; browser and automated evidence cover them. |
| Information density | PARTIALLY_MATCHED | The real page keeps every required Evidence and Trace field visible, so its right panel is denser than the reference. |
| Trace form | PARTIALLY_MATCHED | The reference implies a broader visual treatment; P4C uses a compact vertical sequence because it makes all six real steps readable at the desktop height. |
| Technical metadata emphasis | MATCHED | IDs, provider mode, and endpoints are retained but secondary; developer details are collapsed. |
| Unsupported production claims | MATCHED | No external model, online-agent count, payment, shipping, refund, or trace-history claim is present. |

