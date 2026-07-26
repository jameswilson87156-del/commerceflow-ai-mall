# P4E Visual Comparison

| Area | P4D state | P4E final state | Result |
| --- | --- | --- | --- |
| Sidebar | Present | Present, unchanged | MATCHED |
| SKU selector | Five real SKU cards, but larger cards consumed more vertical space | All five real cards visible in the first `1080px` viewport | MATCHED |
| Chat work area | Correct factual answer but more unused vertical space | User question, answer, Provider metadata, and composer are visible together | MATCHED |
| Provider | Real Provider result | Real `MOCK` or `FALLBACK` result remains visible | MATCHED |
| Business Facts | Real Java facts | Real Java facts remain visible with tighter fact tiles | MATCHED |
| Evidence | Seven Java Evidence entries | Seven entries remain visible in the first viewport | MATCHED |
| Trace | Six backend steps | Six steps remain visible; detail is one readable line with full text available in a title | MATCHED |
| Long technical values | Could dominate a narrow row | Constrained inside their local field without changing the returned value | MATCHED |
| Data authenticity | Local Showcase API data | Same local Showcase API data; no hard-coded business number added | MATCHED |
| Screenshot contract | Older capture surface was not the requested pixels | Native `1920 x 1080`, DPR 1 final captures | MATCHED |

The final page intentionally stays a compact operational workbench. It does not add padding or fake dashboards merely to fill unused space. Payment, shipment, logistics, refund, order-write, Trace-history search, and external AI calls remain outside P4.
