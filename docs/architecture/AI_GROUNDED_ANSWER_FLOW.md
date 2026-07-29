# AI Grounded Answer Flow

```mermaid
sequenceDiagram
  participant C as Web or Mobile client
  participant J as Java mall-api
  participant M as MySQL
  participant P as FastAPI
  C->>J: question + selected product/SKU
  J->>M: query product, SKU, inventory
  J->>J: build restricted businessFacts
  J->>P: structured internal request
  P-->>J: structured answer suggestion
  J->>J: validate, fallback if necessary
  J->>M: save Evidence and Trace
  J-->>C: answer, provider, Evidence, Trace
```

Java is the business-fact source. `businessFacts` includes the selected product/SKU, available stock, price as a string, currency, question, snippets, and query time; it excludes unrelated personal data. FastAPI cannot write the business database, invent inventory, or callback Java. Normal Showcase mode uses deterministic `commerceflow-mock`; Java fact fallback is an explicit result, not a fabricated provider answer.
