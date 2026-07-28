# P6C Evidence And Trace

Evidence is rendered from the Java response without client-side derivation. The verified response contained product name/status, SKU code/color/size, sale price, and current inventory; each row keeps its returned source type and source ID.

The Trace is collapsed by default and displays the returned six steps: receive request, load business facts, call local AI, provider returns, validate structured response, and compose response/Evidence. No SQL, prompt, stack trace, credential, or authentication detail is shown.
